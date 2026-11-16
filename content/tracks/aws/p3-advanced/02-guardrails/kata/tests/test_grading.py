import pytest

from dojo_aws import iam

DEV_ACCOUNT = "111122223333"
ANALYTICS_ACCOUNT = "444455556666"
DATA_ACCOUNT = "777788889999"
ORG = "o-acme1234567"
FULL_ACCESS = {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": "*", "Resource": "*"}]}
DEVELOPER = f"arn:aws:iam::{DEV_ACCOUNT}:role/developer"
ORG_ADMIN = f"arn:aws:iam::{DEV_ACCOUNT}:role/OrgAdmin"
BOUNDARY_ARN = f"arn:aws:iam::{DEV_ACCOUNT}:policy/app-boundary"
BUCKET = "arn:aws:s3:::acme-shared-datasets"


@pytest.fixture(scope="module")
def scp():
    return iam.load("scp-guardrails.json")


@pytest.fixture(scope="module")
def developer():
    return iam.load("developer-policy.json")


@pytest.fixture(scope="module")
def boundary():
    return iam.load("app-boundary.json")


@pytest.fixture(scope="module")
def bucket():
    return iam.load("bucket-policy.json")


@pytest.mark.parametrize("name,kind", [("scp-guardrails.json", "identity"), ("developer-policy.json", "identity"),
                                       ("app-boundary.json", "identity"), ("bucket-policy.json", "resource")])
def test_policies_are_well_formed(name, kind):
    assert iam.validate(iam.load(name), kind=kind) == []


# ---- the SCP: as attached to an OU, next to FullAWSAccess -------------------------------

def under_scp(scp, principal, action, region, resource="*"):
    request = iam.Request(principal, action, resource,
                          context={"aws:RequestedRegion": region, "aws:PrincipalArn": principal})
    return iam.evaluate(request, identity=[FULL_ACCESS], scps=[FULL_ACCESS, [FULL_ACCESS, scp]])


@pytest.mark.parametrize("action,region", [
    ("s3:PutObject", "eu-west-1"), ("lambda:CreateFunction", "eu-central-1"),
    ("iam:CreateRole", "us-east-1"), ("sts:AssumeRole", "us-east-1"),
    ("cloudfront:CreateInvalidation", "us-east-1"), ("route53:ChangeResourceRecordSets", "us-east-1"),
    ("support:CreateCase", "us-east-1"),
])
def test_scp_allows_eu_regions_and_global_services(scp, action, region):
    assert under_scp(scp, DEVELOPER, action, region) == iam.ALLOW, f"{action} in {region} must stay possible"


@pytest.mark.parametrize("action,region", [
    ("ec2:RunInstances", "us-east-1"), ("s3:CreateBucket", "ap-southeast-2"), ("dynamodb:CreateTable", "sa-east-1"),
])
def test_scp_denies_other_regions(scp, action, region):
    assert under_scp(scp, DEVELOPER, action, region) == iam.EXPLICIT_DENY


def test_scp_lets_org_admin_work_in_any_region(scp):
    assert under_scp(scp, ORG_ADMIN, "ec2:RunInstances", "us-east-1") == iam.ALLOW


@pytest.mark.parametrize("principal", [DEVELOPER, ORG_ADMIN])
@pytest.mark.parametrize("action", ["cloudtrail:StopLogging", "cloudtrail:DeleteTrail",
                                    "organizations:LeaveOrganization"])
def test_scp_protects_the_trail_and_the_organization_from_everyone(scp, principal, action):
    assert under_scp(scp, principal, action, "eu-west-1") == iam.EXPLICIT_DENY


# ---- delegating role creation with a permissions boundary --------------------------------

def as_developer(developer, action, resource, **context):
    return iam.evaluate(iam.Request(DEVELOPER, action, resource, context=context), identity=[developer])


def test_developers_create_app_roles_with_the_boundary(developer):
    found = as_developer(developer, "iam:CreateRole", f"arn:aws:iam::{DEV_ACCOUNT}:role/app/orders-api",
                         **{"iam:PermissionsBoundary": BOUNDARY_ARN})
    assert found == iam.ALLOW


@pytest.mark.parametrize("resource,context", [
    (f"arn:aws:iam::{DEV_ACCOUNT}:role/app/orders-api", {}),
    (f"arn:aws:iam::{DEV_ACCOUNT}:role/app/orders-api",
     {"iam:PermissionsBoundary": f"arn:aws:iam::{DEV_ACCOUNT}:policy/anything"}),
    (f"arn:aws:iam::{DEV_ACCOUNT}:role/OrgAdmin2", {"iam:PermissionsBoundary": BOUNDARY_ARN}),
], ids=["no-boundary", "other-boundary", "outside-app-path"])
def test_developers_cannot_create_roles_without_the_boundary(developer, resource, context):
    assert as_developer(developer, "iam:CreateRole", resource, **context) != iam.ALLOW


@pytest.mark.parametrize("action", ["iam:DeleteRolePermissionsBoundary", "iam:PutRolePermissionsBoundary",
                                    "iam:CreatePolicyVersion", "iam:DeletePolicy"])
def test_developers_can_never_loosen_the_boundary(developer, action):
    found = as_developer(developer, action, BOUNDARY_ARN, **{"iam:PermissionsBoundary": BOUNDARY_ARN})
    assert found == iam.EXPLICIT_DENY, f"deny {action} explicitly"


@pytest.mark.parametrize("action", ["iam:CreateUser", "iam:CreateAccessKey", "iam:AttachUserPolicy"])
def test_developers_get_no_other_iam_power(developer, action):
    assert as_developer(developer, action, f"arn:aws:iam::{DEV_ACCOUNT}:user/ghost") != iam.ALLOW


def test_developers_pass_only_app_roles(developer):
    assert as_developer(developer, "iam:PassRole", f"arn:aws:iam::{DEV_ACCOUNT}:role/app/orders-api") == iam.ALLOW
    assert as_developer(developer, "iam:PassRole", ORG_ADMIN) != iam.ALLOW


def app_role(boundary, action, resource="*"):
    role = f"arn:aws:iam::{DEV_ACCOUNT}:role/app/orders-api"
    return iam.evaluate(iam.Request(role, action, resource), identity=[FULL_ACCESS], boundary=boundary)


@pytest.mark.parametrize("action", ["s3:GetObject", "dynamodb:Query", "sqs:SendMessage", "logs:PutLogEvents"])
def test_the_boundary_keeps_what_apps_need(boundary, action):
    assert app_role(boundary, action) == iam.ALLOW


@pytest.mark.parametrize("action", ["iam:CreateUser", "iam:AttachRolePolicy", "s3:DeleteBucket",
                                    "dynamodb:DeleteTable", "ec2:RunInstances", "kms:ScheduleKeyDeletion"])
def test_the_boundary_caps_an_app_role_even_with_admin_policies(boundary, action):
    assert app_role(boundary, action) != iam.ALLOW, f"an app role must never get {action}"


def test_the_boundary_names_actions_without_wildcards(boundary):
    actions = [a for s in iam.statements(boundary) for a in iam.as_list(s.get("Action"))]
    assert actions and all("*" not in a for a in actions), f"list exact actions, found {actions}"


# ---- the cross-account bucket policy, in the data account --------------------------------

def read_dataset(bucket, principal, identity, action="s3:GetObject", resource=f"{BUCKET}/2026/sales.parquet",
                 org=ORG, tls="true"):
    request = iam.Request(principal, action, resource, resource_account=DATA_ACCOUNT,
                          context={"aws:PrincipalOrgID": org, "aws:SecureTransport": tls})
    return iam.evaluate(request, identity=identity, resource_policy=bucket)


ANALYTICS = f"arn:aws:iam::{ANALYTICS_ACCOUNT}:role/analytics"
READS = {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": ["s3:GetObject", "s3:ListBucket"],
                                                  "Resource": [BUCKET, f"{BUCKET}/*"]}]}


def test_the_analytics_role_reads_and_lists(bucket):
    assert read_dataset(bucket, ANALYTICS, [READS]) == iam.ALLOW
    assert read_dataset(bucket, ANALYTICS, [READS], action="s3:ListBucket", resource=BUCKET) == iam.ALLOW


def test_cross_account_access_also_needs_the_callers_own_policy(bucket):
    assert read_dataset(bucket, ANALYTICS, []) != iam.ALLOW


@pytest.mark.parametrize("principal,identity,extra", [
    (f"arn:aws:iam::{ANALYTICS_ACCOUNT}:role/intern", [FULL_ACCESS], {}),
    (f"arn:aws:iam::{DEV_ACCOUNT}:role/developer", [FULL_ACCESS], {}),
    ("arn:aws:iam::999999999999:role/outsider", [FULL_ACCESS], {"org": "o-other000000"}),
    (ANALYTICS, [FULL_ACCESS], {"action": "s3:PutObject"}),
    (ANALYTICS, [FULL_ACCESS], {"action": "s3:DeleteObject"}),
], ids=["other-role-same-account", "other-account-in-org", "outside-org", "write", "delete"])
def test_nobody_else_gets_in_and_nobody_writes(bucket, principal, identity, extra):
    assert read_dataset(bucket, principal, identity, **extra) != iam.ALLOW


def test_denies_principals_outside_the_organization_explicitly(bucket):
    found = read_dataset(bucket, ANALYTICS, [READS], org="o-other000000")
    assert found == iam.EXPLICIT_DENY, "add a Deny for aws:PrincipalOrgID other than o-acme1234567"


def test_denies_requests_without_tls_explicitly(bucket):
    assert read_dataset(bucket, ANALYTICS, [READS], tls="false") == iam.EXPLICIT_DENY
