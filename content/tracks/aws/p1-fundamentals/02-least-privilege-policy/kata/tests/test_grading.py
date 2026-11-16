import pytest

from dojo_aws import iam

ACCOUNT = "111122223333"
ROLE = f"arn:aws:iam::{ACCOUNT}:role/invoice-service"
BUCKET = "arn:aws:s3:::acme-invoices"
TABLE = f"arn:aws:dynamodb:eu-west-1:{ACCOUNT}:table/Invoices"
QUEUE = f"arn:aws:sqs:eu-west-1:{ACCOUNT}:invoice-events"
TLS = {"aws:SecureTransport": "true"}

# (action, resource, extra context): what the invoice service does every day.
NEEDED = [
    ("s3:GetObject", f"{BUCKET}/invoices/2026/0042.pdf", {}),
    ("s3:PutObject", f"{BUCKET}/invoices/2026/0043.pdf", {}),
    ("s3:ListBucket", BUCKET, {"s3:prefix": "invoices/2026/"}),
    ("dynamodb:GetItem", TABLE, {}),
    ("dynamodb:PutItem", TABLE, {}),
    ("dynamodb:UpdateItem", TABLE, {}),
    ("dynamodb:Query", TABLE, {}),
    ("dynamodb:Query", f"{TABLE}/index/by-customer", {}),
    ("sqs:SendMessage", QUEUE, {}),
]

# What it must not be able to do, even though the starter allowed it.
FORBIDDEN = [
    ("s3:DeleteObject", f"{BUCKET}/invoices/2026/0042.pdf", {}),
    ("s3:GetObject", f"{BUCKET}/payroll/2026-10.csv", {}),
    ("s3:GetObject", "arn:aws:s3:::acme-backups/db.dump", {}),
    ("s3:ListBucket", BUCKET, {"s3:prefix": "payroll/"}),
    ("s3:ListBucket", BUCKET, {}),
    ("s3:PutBucketPolicy", BUCKET, {}),
    ("dynamodb:DeleteItem", TABLE, {}),
    ("dynamodb:Scan", TABLE, {}),
    ("dynamodb:DeleteTable", TABLE, {}),
    ("dynamodb:GetItem", f"arn:aws:dynamodb:eu-west-1:{ACCOUNT}:table/Customers", {}),
    ("sqs:ReceiveMessage", QUEUE, {}),
    ("sqs:SendMessage", f"arn:aws:sqs:eu-west-1:{ACCOUNT}:payments", {}),
    ("iam:PassRole", f"arn:aws:iam::{ACCOUNT}:role/admin", {}),
]


@pytest.fixture(scope="module")
def policy():
    return iam.load("policy.json")


@pytest.fixture(scope="module")
def trust():
    return iam.load("trust-policy.json")


def decision(policy, action, resource, context):
    request = iam.Request(ROLE, action, resource, context={**TLS, **context})
    return iam.evaluate(request, identity=[policy])


def test_policy_is_well_formed(policy):
    assert iam.validate(policy) == []


@pytest.mark.parametrize("action,resource,context", NEEDED,
                         ids=[f"{a}:{r.rsplit(':', 1)[-1]}" for a, r, _ in NEEDED])
def test_allows_what_the_service_needs(policy, action, resource, context):
    assert decision(policy, action, resource, context) == iam.ALLOW, (
        f"{action} on {resource} must be allowed"
    )


@pytest.mark.parametrize("action,resource,context", FORBIDDEN,
                         ids=[f"{a}:{r.rsplit(':', 1)[-1]}:{c}" for a, r, c in FORBIDDEN])
def test_denies_everything_else(policy, action, resource, context):
    assert decision(policy, action, resource, context) != iam.ALLOW, (
        f"{action} on {resource} {context or ''} must not be allowed"
    )


def test_names_actions_without_wildcards(policy):
    wild = [action for statement in iam.statements(policy) if statement.get("Effect") == "Allow"
            for action in iam.as_list(statement.get("Action")) if "*" in action]
    assert wild == [], f"list the exact actions instead of {wild}"
    assert all("NotAction" not in s for s in iam.statements(policy) if s.get("Effect") == "Allow"), (
        "an Allow with NotAction grants almost everything"
    )


def test_explicitly_denies_requests_without_tls(policy):
    request = iam.Request(ROLE, "s3:GetObject", f"{BUCKET}/invoices/2026/0042.pdf",
                          context={"aws:SecureTransport": "false"})
    assert iam.evaluate(request, identity=[policy]) == iam.EXPLICIT_DENY, (
        "add a Deny statement for requests where aws:SecureTransport is false"
    )


def test_trust_policy_is_well_formed(trust):
    assert iam.validate(trust, kind="resource") == []


def test_ecs_tasks_of_this_account_can_assume_the_role(trust):
    request = iam.Request("ecs-tasks.amazonaws.com", "sts:AssumeRole", ROLE, context={
        "aws:SourceAccount": ACCOUNT,
        "aws:SourceArn": f"arn:aws:ecs:eu-west-1:{ACCOUNT}:task/invoices/0123456789abcdef",
    })
    assert iam.evaluate(request, resource_policy=trust) == iam.ALLOW


@pytest.mark.parametrize("principal,context", [
    ("arn:aws:iam::999988887777:role/attacker", {}),
    (f"arn:aws:iam::{ACCOUNT}:user/intern", {}),
    ("lambda.amazonaws.com", {"aws:SourceAccount": ACCOUNT}),
    ("ecs-tasks.amazonaws.com", {"aws:SourceAccount": "999988887777",
                                 "aws:SourceArn": "arn:aws:ecs:eu-west-1:999988887777:task/x/1"}),
], ids=["other-account", "user-in-account", "lambda", "ecs-of-another-account"])
def test_nobody_else_can_assume_the_role(trust, principal, context):
    # Assume the worst: the caller's own policies allow it everything.
    admin = {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": "*", "Resource": "*"}]}
    request = iam.Request(principal, "sts:AssumeRole", ROLE, context=context)
    assert iam.evaluate(request, identity=[admin], resource_policy=trust) != iam.ALLOW, (
        f"{principal} must not be able to assume the role"
    )
