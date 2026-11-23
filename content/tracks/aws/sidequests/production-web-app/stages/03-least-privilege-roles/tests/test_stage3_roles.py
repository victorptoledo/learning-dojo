import pytest

from dojo_aws import getatt, iam
from dojo_aws.stack import ACCOUNT
from webapp import one, props, stack

ROLE_ARN = f"arn:aws:iam::{ACCOUNT}:role/dojo-InstanceRole"
SESSION = f"arn:aws:sts::{ACCOUNT}:assumed-role/dojo-InstanceRole/i-0abc123"
SECRET = f"arn:aws:secretsmanager:eu-west-1:{ACCOUNT}:secret:rds!db-database-AbCdEf"
KEY = f"arn:aws:kms:eu-west-1:{ACCOUNT}:key/datakey-0000-4000-8000-000000000000"
LOGS = f"arn:aws:logs:eu-west-1:{ACCOUNT}:log-group:dojo-AppLogGroup"
VIA_SECRETS = {"kms:ViaService": "secretsmanager.eu-west-1.amazonaws.com"}
SSM_CORE = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"


@pytest.fixture(scope="module")
def role():
    spec = stack().value(props("AWS::IAM::Role", "InstanceRole"))
    assert spec.get("ManagedPolicyArns") == [SSM_CORE], (
        f"InstanceRole's only managed policy is AmazonSSMManagedInstanceCore, found {spec.get('ManagedPolicyArns')}")
    documents = [p.get("PolicyDocument") for p in spec.get("Policies", [])]
    assert len(documents) == 1, f"give InstanceRole one inline policy, found {len(documents)}"
    for document in documents:
        assert iam.validate(document) == [], "; ".join(iam.validate(document))
    return spec["AssumeRolePolicyDocument"], documents


def test_the_logs_are_kept_90_days():
    assert int(props("AWS::Logs::LogGroup", "AppLogGroup").get("RetentionInDays", 0)) == 90


def test_the_instances_run_with_the_role():
    profile = props("AWS::IAM::InstanceProfile", "InstanceProfile")
    assert stack().value(profile.get("Roles")) == ["dojo-InstanceRole"], "InstanceProfile: Roles [!Ref InstanceRole]"
    data = props("AWS::EC2::LaunchTemplate", "LaunchTemplate").get("LaunchTemplateData", {})
    assert getatt((data.get("IamInstanceProfile") or {}).get("Arn")) == ("InstanceProfile", "Arn"), (
        "the launch template needs IamInstanceProfile: { Arn: !GetAtt InstanceProfile.Arn }")


@pytest.mark.parametrize("principal,expected", [
    ("ec2.amazonaws.com", iam.ALLOW),
    ("lambda.amazonaws.com", iam.IMPLICIT_DENY),
    (f"arn:aws:iam::{ACCOUNT}:user/dev", iam.IMPLICIT_DENY),
    ("arn:aws:iam::444455556666:root", iam.IMPLICIT_DENY),
])
def test_only_ec2_assumes_the_role(role, principal, expected):
    request = iam.Request(principal=principal, action="sts:AssumeRole", resource=ROLE_ARN)
    found = iam.evaluate(request, identity=[{"Version": "2012-10-17", "Statement": [
        {"Effect": "Allow", "Action": "sts:AssumeRole", "Resource": "*"}]}], resource_policy=role[0])
    assert found == expected, f"{principal} assuming the role: expected {expected}, got {found}"


@pytest.mark.parametrize("case,action,resource,context", [
    ("read the database secret", "secretsmanager:GetSecretValue", SECRET, {}),
    ("decrypt it through Secrets Manager", "kms:Decrypt", KEY, VIA_SECRETS),
    ("download a release", "s3:GetObject", "arn:aws:s3:::shop-artifacts/releases/2026-11-23/app.tar.gz", {}),
    ("create a log stream", "logs:CreateLogStream", f"{LOGS}:log-stream:i-0abc123", {}),
    ("write logs", "logs:PutLogEvents", f"{LOGS}:log-stream:i-0abc123", {}),
])
def test_the_app_may_do_its_job(role, case, action, resource, context):
    request = iam.Request(principal=SESSION, action=action, resource=resource, context=context)
    found = iam.evaluate(request, identity=role[1])
    assert found == iam.ALLOW, f"{case}: {action} on {resource} must be allowed, got {found}"


@pytest.mark.parametrize("case,action,resource,context", [
    ("another secret", "secretsmanager:GetSecretValue",
     f"arn:aws:secretsmanager:eu-west-1:{ACCOUNT}:secret:payments/stripe-Xy12Ab", {}),
    ("change the secret", "secretsmanager:PutSecretValue", SECRET, {}),
    ("decrypt directly", "kms:Decrypt", KEY, {}),
    ("decrypt through another service", "kms:Decrypt", KEY, {"kms:ViaService": "rds.eu-west-1.amazonaws.com"}),
    ("decrypt through Secrets Manager elsewhere", "kms:Decrypt", KEY,
     {"kms:ViaService": "secretsmanager.us-east-1.amazonaws.com"}),
    ("encrypt with the key", "kms:Encrypt", KEY, VIA_SECRETS),
    ("another key", "kms:Decrypt", f"arn:aws:kms:eu-west-1:{ACCOUNT}:key/other", VIA_SECRETS),
    ("a build outside releases/", "s3:GetObject", "arn:aws:s3:::shop-artifacts/secrets/deploy.env", {}),
    ("another bucket's releases", "s3:GetObject", "arn:aws:s3:::other-artifacts/releases/app.tar.gz", {}),
    ("upload a build", "s3:PutObject", "arn:aws:s3:::shop-artifacts/releases/app.tar.gz", {}),
    ("list the bucket", "s3:ListBucket", "arn:aws:s3:::shop-artifacts", {}),
    ("another log group", "logs:PutLogEvents",
     f"arn:aws:logs:eu-west-1:{ACCOUNT}:log-group:payments:log-stream:x", {}),
    ("delete the logs", "logs:DeleteLogGroup", f"{LOGS}:*", {}),
    ("pass a role", "iam:PassRole", f"arn:aws:iam::{ACCOUNT}:role/admin", {}),
    ("start instances", "ec2:RunInstances", "*", {}),
    ("assume another role", "sts:AssumeRole", f"arn:aws:iam::{ACCOUNT}:role/admin", {}),
    ("read the database", "rds:DescribeDBInstances", "*", {}),
])
def test_the_app_may_do_nothing_else(role, case, action, resource, context):
    request = iam.Request(principal=SESSION, action=action, resource=resource, context=context)
    found = iam.evaluate(request, identity=role[1])
    assert found != iam.ALLOW, f"{case}: {action} on {resource} must not be allowed"


def test_no_wildcard_actions_or_resources(role):
    for document in role[1]:
        for statement in iam.statements(document):
            for action in iam.as_list(statement.get("Action")):
                assert "*" not in action, f"no wildcard actions: {action}"
            assert "NotAction" not in statement and "NotResource" not in statement, "no NotAction or NotResource"
            for resource in iam.as_list(statement.get("Resource")):
                assert resource != "*", f"name every resource: {statement.get('Action')} has Resource *"


def test_the_role_has_no_long_lived_keys():
    assert not stack().resources_of("AWS::IAM::AccessKey"), "no access keys: the instances use the role"
    assert not stack().resources_of("AWS::IAM::User"), "no IAM users in an application stack"
    one("AWS::IAM::Role", "InstanceRole")
