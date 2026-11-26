import pytest

from dojo_aws import iam, ref
from dojo_aws.stack import ACCOUNT
from webapp import ingress, one, props, stack, tpl

DB = "AWS::RDS::DBInstance"
KEY_ARN = f"arn:aws:kms:eu-west-1:{ACCOUNT}:key/datakey-0000-4000-8000-000000000000"
ADMIN = f"arn:aws:iam::{ACCOUNT}:role/platform-admin"
OUTSIDER = "arn:aws:iam::444455556666:role/admin"
KMS_ADMIN = {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": "kms:*", "Resource": "*"}]}


@pytest.fixture(scope="module")
def db():
    return props(DB, "Database")


def test_the_database_class_is_a_parameter():
    spec = tpl().get("Parameters", {}).get("DatabaseClass", {})
    assert spec.get("AllowedValues") == ["db.t4g.medium", "db.m7g.large"], "DatabaseClass AllowedValues"
    assert spec.get("Default") == "db.t4g.medium"
    assert tpl()["Parameters"].get("DataSubnetIds", {}).get("Type") == "List<AWS::EC2::Subnet::Id>"


def test_postgres_17_on_gp3_with_the_chosen_class(db):
    assert db.get("Engine") == "postgres", "Engine postgres"
    assert str(db.get("EngineVersion", "")).startswith("17."), "pin a PostgreSQL 17 EngineVersion"
    assert ref(db.get("DBInstanceClass")) == "DatabaseClass", "DBInstanceClass must be !Ref DatabaseClass"
    assert int(db.get("AllocatedStorage", 0)) == 50 and db.get("StorageType") == "gp3", "50 GiB of gp3"


def test_the_database_survives_losing_a_zone(db):
    assert db.get("MultiAZ") is True, "MultiAZ: true (a standby in another zone, automatic failover)"


def test_the_database_is_encrypted_with_the_companys_key(db):
    assert db.get("StorageEncrypted") is True, "StorageEncrypted: true"
    assert ref(db.get("KmsKeyId")) == "DataKey", "KmsKeyId must be DataKey, not the AWS managed key"


def test_the_database_is_private(db):
    assert db.get("PubliclyAccessible") is False, "PubliclyAccessible: false"
    assert ref(db.get("DBSubnetGroupName")) == "DatabaseSubnetGroup"
    group = props("AWS::RDS::DBSubnetGroup", "DatabaseSubnetGroup")
    assert stack().value(group.get("SubnetIds")) == ["subnet-data-a", "subnet-data-b", "subnet-data-c"], (
        "the subnet group holds !Ref DataSubnetIds")
    assert stack().value(db.get("VPCSecurityGroups")) == ["dojo-DatabaseSecurityGroup"], (
        "VPCSecurityGroups: [!GetAtt DatabaseSecurityGroup.GroupId]")
    expected = [{"protocol": "tcp", "ports": (5432, 5432), "group": "AppSecurityGroup"}]
    assert ingress("DatabaseSecurityGroup") == expected, "DatabaseSecurityGroup: tcp 5432 from AppSecurityGroup only"


def test_the_data_is_protected_from_mistakes(db):
    resource = one(DB, "Database")
    assert db.get("DeletionProtection") is True, "DeletionProtection: true"
    assert int(db.get("BackupRetentionPeriod", 1)) == 14, "keep backups 14 days"
    assert db.get("CopyTagsToSnapshot") is True, "CopyTagsToSnapshot: true"
    assert db.get("AutoMinorVersionUpgrade", True) is True, "upgrade minor versions automatically"
    assert resource.get("DeletionPolicy") == "Snapshot", "DeletionPolicy: Snapshot"
    assert resource.get("UpdateReplacePolicy") == "Snapshot", "UpdateReplacePolicy: Snapshot"


def test_no_password_anywhere(db):
    assert db.get("ManageMasterUserPassword") is True, "ManageMasterUserPassword: true"
    assert "MasterUserPassword" not in db, "never put MasterUserPassword in the template"
    assert ref((db.get("MasterUserSecret") or {}).get("KmsKeyId")) == "DataKey", (
        "encrypt the managed secret with DataKey: MasterUserSecret: { KmsKeyId: !Ref DataKey }")
    for name, spec in (tpl().get("Parameters") or {}).items():
        assert "password" not in name.lower(), f"no password parameter: {name}"


@pytest.fixture(scope="module")
def key():
    resource = one("AWS::KMS::Key", "DataKey")
    return resource, stack().value(resource["Properties"].get("KeyPolicy"))


def test_the_key_rotates_and_outlives_the_stack(key):
    resource, _ = key
    spec = resource["Properties"]
    assert spec.get("EnableKeyRotation") is True, "EnableKeyRotation: true"
    assert int(spec.get("PendingWindowInDays", 30)) == 30, "a 30-day deletion window"
    assert resource.get("DeletionPolicy") == "Retain" and resource.get("UpdateReplacePolicy") == "Retain", (
        "keep the key when the stack goes: DeletionPolicy and UpdateReplacePolicy Retain")


def test_the_key_has_an_alias():
    spec = props("AWS::KMS::Alias", "DataKeyAlias")
    assert stack().value(spec.get("AliasName")) == "alias/dojo-data", 'AliasName: !Sub "alias/${AWS::StackName}-data"'
    assert ref(spec.get("TargetKeyId")) == "DataKey"


def test_the_key_policy_is_well_formed(key):
    _, policy = key
    assert iam.validate(policy, "resource") == [], "; ".join(iam.validate(policy, "resource"))


@pytest.mark.parametrize("case,principal,identity,action,expected", [
    ("an admin of the account manages the key", ADMIN, [KMS_ADMIN], "kms:PutKeyPolicy", iam.ALLOW),
    ("an admin of the account decrypts", ADMIN, [KMS_ADMIN], "kms:Decrypt", iam.ALLOW),
    ("the account root only enables IAM policies", ADMIN, [], "kms:Decrypt", iam.IMPLICIT_DENY),
    ("another account's admin", OUTSIDER, [KMS_ADMIN], "kms:Decrypt", iam.IMPLICIT_DENY),
    ("another account schedules deletion", OUTSIDER, [KMS_ADMIN], "kms:ScheduleKeyDeletion", iam.IMPLICIT_DENY),
])
def test_only_this_account_uses_the_key(key, case, principal, identity, action, expected):
    _, policy = key
    request = iam.Request(principal=principal, action=action, resource=KEY_ARN, resource_account=ACCOUNT)
    found = iam.evaluate(request, identity=identity, resource_policy=policy)
    assert found == expected, f"{case}: expected {expected}, the key policy gives {found}"


def test_the_key_policy_names_no_one_outside(key):
    _, policy = key
    for statement in iam.statements(policy):
        principal = statement.get("Principal")
        assert principal != "*" and principal != {"AWS": "*"}, "no statement may open the key to everyone"
