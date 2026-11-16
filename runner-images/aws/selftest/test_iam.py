"""Self-tests of the dojo's IAM evaluator, run when the image is built.

Each case follows a rule of the IAM User Guide ("Policy evaluation logic", "Condition
operators", "Policy variables"), so a change that breaks AWS's semantics fails the build.
"""

import pytest

from dojo_aws import iam

A = "111122223333"
B = "444455556666"
ROLE = f"arn:aws:iam::{A}:role/app"
OTHER = f"arn:aws:iam::{B}:role/reader"


def policy(*statements):
    return {"Version": "2012-10-17", "Statement": list(statements)}


def allow(action="*", resource="*", **extra):
    return {"Effect": "Allow", "Action": action, "Resource": resource, **extra}


def deny(action="*", resource="*", **extra):
    return {"Effect": "Deny", "Action": action, "Resource": resource, **extra}


ADMIN = policy(allow())


def req(action, resource="*", principal=ROLE, **context):
    return iam.Request(principal, action, resource, context=context)


def test_nothing_is_allowed_by_default():
    assert iam.evaluate(req("s3:GetObject"), identity=[]) == iam.IMPLICIT_DENY


def test_an_explicit_deny_beats_any_allow():
    found = iam.evaluate(req("s3:DeleteObject"), identity=[ADMIN, policy(deny("s3:Delete*"))])
    assert found == iam.EXPLICIT_DENY


def test_actions_match_case_insensitively_with_wildcards():
    assert iam.allowed(req("S3:getobject"), identity=[policy(allow("s3:Get*"))])
    assert not iam.allowed(req("s3:PutObject"), identity=[policy(allow("s3:Get*"))])
    assert iam.allowed(req("s3:GetObject"), identity=[policy(allow("s3:Get?bject"))])


def test_resources_match_case_sensitively():
    found = policy(allow("s3:GetObject", "arn:aws:s3:::Reports/*"))
    assert not iam.allowed(req("s3:GetObject", "arn:aws:s3:::reports/a"), identity=[found])


def test_not_action_and_not_resource():
    everything_but_iam = policy({"Effect": "Allow", "NotAction": "iam:*", "Resource": "*"})
    assert iam.allowed(req("s3:GetObject"), identity=[everything_but_iam])
    assert not iam.allowed(req("iam:CreateUser"), identity=[everything_but_iam])
    outside = policy({"Effect": "Deny", "Action": "*", "NotResource": "arn:aws:s3:::safe/*"})
    assert iam.evaluate(req("s3:GetObject", "arn:aws:s3:::other/x"), identity=[ADMIN, outside]) == iam.EXPLICIT_DENY


def test_condition_keys_are_anded_and_values_ored():
    found = policy(allow(Condition={"StringEquals": {"aws:RequestedRegion": ["eu-west-1", "eu-central-1"]},
                                    "Bool": {"aws:SecureTransport": "true"}}))
    assert iam.allowed(req("s3:GetObject", **{"aws:RequestedRegion": "eu-central-1", "aws:SecureTransport": "true"}),
                       identity=[found])
    assert not iam.allowed(req("s3:GetObject", **{"aws:RequestedRegion": "eu-central-1"}), identity=[found])


def test_a_missing_key_fails_positive_operators_and_passes_negated_ones():
    positive = policy(allow(Condition={"StringEquals": {"aws:PrincipalTag/team": "data"}}))
    negated = policy(allow(Condition={"StringNotEquals": {"aws:PrincipalTag/team": "data"}}))
    assert not iam.allowed(req("s3:GetObject"), identity=[positive])
    assert iam.allowed(req("s3:GetObject"), identity=[negated])


def test_if_exists_and_null():
    found = policy(allow(Condition={"StringEqualsIfExists": {"ec2:InstanceType": "t4g.micro"}}))
    assert iam.allowed(req("ec2:RunInstances"), identity=[found])
    assert not iam.allowed(req("ec2:RunInstances", **{"ec2:InstanceType": "m7g.large"}), identity=[found])
    absent = policy(deny(Condition={"Null": {"aws:MultiFactorAuthAge": "true"}}))
    assert iam.evaluate(req("iam:CreateUser"), identity=[ADMIN, absent]) == iam.EXPLICIT_DENY
    assert iam.allowed(req("iam:CreateUser", **{"aws:MultiFactorAuthAge": "30"}), identity=[ADMIN, absent])


def test_set_operators():
    any_of = policy(allow(Condition={"ForAnyValue:StringEquals": {"aws:TagKeys": ["env", "team"]}}))
    all_of = policy(allow(Condition={"ForAllValues:StringEquals": {"aws:TagKeys": ["env", "team"]}}))
    assert iam.allowed(req("ec2:CreateTags", **{"aws:TagKeys": ["env", "owner"]}), identity=[any_of])
    assert not iam.allowed(req("ec2:CreateTags", **{"aws:TagKeys": ["env", "owner"]}), identity=[all_of])
    assert iam.allowed(req("ec2:CreateTags"), identity=[all_of]), "ForAllValues is true for a missing key"


def test_ip_numeric_date_and_arn_operators():
    found = policy(allow(Condition={"IpAddress": {"aws:SourceIp": "203.0.113.0/24"},
                                    "NumericLessThanEquals": {"s3:max-keys": "100"},
                                    "DateLessThan": {"aws:CurrentTime": "2027-01-01T00:00:00Z"},
                                    "ArnLike": {"aws:PrincipalArn": f"arn:aws:iam::{A}:role/*"}}))
    good = {"aws:SourceIp": "203.0.113.9", "s3:max-keys": "50", "aws:CurrentTime": "2026-11-15T10:00:00Z",
            "aws:PrincipalArn": ROLE}
    assert iam.allowed(req("s3:ListBucket", **good), identity=[found])
    assert not iam.allowed(req("s3:ListBucket", **{**good, "aws:SourceIp": "198.51.100.1"}), identity=[found])


def test_policy_variables():
    found = policy(allow("s3:*", "arn:aws:s3:::home/${aws:username}/*"))
    assert iam.allowed(req("s3:GetObject", "arn:aws:s3:::home/ada/notes", **{"aws:username": "ada"}),
                       identity=[found])
    assert not iam.allowed(req("s3:GetObject", "arn:aws:s3:::home/bob/notes", **{"aws:username": "ada"}),
                           identity=[found])
    assert not iam.allowed(req("s3:GetObject", "arn:aws:s3:::home//notes"), identity=[found])


def test_a_boundary_caps_identity_permissions():
    boundary = policy(allow("s3:*"))
    assert iam.allowed(req("s3:GetObject"), identity=[ADMIN], boundary=boundary)
    assert iam.evaluate(req("iam:CreateUser"), identity=[ADMIN], boundary=boundary) == iam.IMPLICIT_DENY
    assert not iam.allowed(req("s3:GetObject"), identity=[], boundary=boundary), "a boundary grants nothing"


def test_every_scp_level_must_allow():
    full = policy(allow())
    eu_only = policy(deny(Condition={"StringNotEquals": {"aws:RequestedRegion": "eu-west-1"}}))
    request = req("s3:GetObject", **{"aws:RequestedRegion": "eu-west-1"})
    assert iam.allowed(request, identity=[ADMIN], scps=[full, [full, eu_only]])
    assert not iam.allowed(request, identity=[ADMIN], scps=[full, policy(allow("ec2:*"))])
    us = req("s3:GetObject", **{"aws:RequestedRegion": "us-east-1"})
    assert iam.evaluate(us, identity=[ADMIN], scps=[[full, eu_only]]) == iam.EXPLICIT_DENY


def test_same_account_resource_policy_alone_is_enough():
    bucket = policy({"Effect": "Allow", "Principal": {"AWS": ROLE}, "Action": "s3:GetObject",
                     "Resource": "arn:aws:s3:::b/*"})
    assert iam.allowed(req("s3:GetObject", "arn:aws:s3:::b/k"), identity=[], resource_policy=bucket)


def test_cross_account_needs_both_sides():
    bucket = policy({"Effect": "Allow", "Principal": {"AWS": OTHER}, "Action": "s3:GetObject",
                     "Resource": "arn:aws:s3:::b/*"})
    reader_policy = policy(allow("s3:GetObject", "arn:aws:s3:::b/*"))
    request = iam.Request(OTHER, "s3:GetObject", "arn:aws:s3:::b/k", resource_account=A)
    assert not iam.allowed(request, identity=[], resource_policy=bucket)
    assert not iam.allowed(request, identity=[reader_policy], resource_policy=None)
    assert iam.allowed(request, identity=[reader_policy], resource_policy=bucket)


def test_account_principals_and_role_sessions():
    account = policy({"Effect": "Allow", "Principal": {"AWS": f"arn:aws:iam::{B}:root"},
                      "Action": "sqs:SendMessage", "Resource": "*"})
    request = iam.Request(f"arn:aws:sts::{B}:assumed-role/reader/session-1", "sqs:SendMessage",
                          f"arn:aws:sqs:eu-west-1:{A}:q")
    assert iam.allowed(request, identity=[ADMIN], resource_policy=account)
    named = policy({"Effect": "Allow", "Principal": {"AWS": OTHER}, "Action": "sqs:SendMessage", "Resource": "*"})
    assert iam.allowed(request, identity=[ADMIN], resource_policy=named)


def test_assume_role_always_needs_the_trust_policy():
    trust = policy({"Effect": "Allow", "Principal": {"Service": "lambda.amazonaws.com"}, "Action": "sts:AssumeRole"})
    assert iam.allowed(iam.Request("lambda.amazonaws.com", "sts:AssumeRole", ROLE), resource_policy=trust)
    assert not iam.allowed(iam.Request(f"arn:aws:iam::{A}:user/admin", "sts:AssumeRole", ROLE),
                           identity=[ADMIN], resource_policy=trust)


def test_service_principals_have_no_identity_policies():
    topic = policy({"Effect": "Allow", "Principal": {"Service": "budgets.amazonaws.com"}, "Action": "sns:Publish",
                    "Resource": "*"})
    events = iam.Request("events.amazonaws.com", "sns:Publish", f"arn:aws:sns:eu-west-1:{A}:t")
    assert not iam.allowed(events, identity=[ADMIN], resource_policy=topic)
    budgets = iam.Request("budgets.amazonaws.com", "sns:Publish", f"arn:aws:sns:eu-west-1:{A}:t")
    assert iam.allowed(budgets, resource_policy=topic)


@pytest.mark.parametrize("broken,problem", [
    ({"Statement": []}, "Version"),
    (policy({"Effect": "Allow", "Action": "s3:GetObject"}), "Resource"),
    (policy({"Effect": "allow", "Action": "s3:GetObject", "Resource": "*"}), "Effect"),
    (policy({"Effect": "Allow", "Action": "GetObject", "Resource": "*"}), "service:Action"),
    (policy({"Effect": "Allow", "Action": "s3:GetObject", "Resource": "*", "Condition": {"StringEqual": {}}}),
     "operator"),
])
def test_validate_reports_broken_policies(broken, problem):
    assert any(problem in message for message in iam.validate(broken))
