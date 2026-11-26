import re

import pytest

from dojo_aws import getatt, iam, ref, resources, template
from dojo_aws.stack import ACCOUNT, REGION, Stack

BACKUP_ACCOUNT = "999900001111"
OUTSIDER = "444455556666"
ORG = "o-acme1234567"
KEY_ADMIN = f"arn:aws:iam::{ACCOUNT}:role/KeyAdmin"
BREAK_GLASS = f"arn:aws:iam::{ACCOUNT}:role/OrgBreakGlass"
ADMIN = f"arn:aws:iam::{ACCOUNT}:role/Admin"
APP = f"arn:aws:iam::{ACCOUNT}:role/orders-app"
COPIER = f"arn:aws:iam::{BACKUP_ACCOUNT}:role/backup-copier"
BACKUP_VIA = f"backup.{REGION}.amazonaws.com"
EVERYTHING = {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": "*", "Resource": "*"}]}
DR_VAULT = f"arn:aws:backup:eu-central-1:{BACKUP_ACCOUNT}:backup-vault:acme-dr"
VAULT_ARN = f"arn:aws:backup:{REGION}:{ACCOUNT}:backup-vault:orders-vault"
KEY_ARN = f"arn:aws:kms:{REGION}:{ACCOUNT}:key/backupkey-0000-4000-8000-000000000000"
CRON_HOURLY = re.compile(r"cron\(\d{1,2} (\*|\*/1|0/1) \S+ \S+ \S+ \S+\)")


@pytest.fixture(scope="module")
def tpl():
    return template("backup.yaml")


@pytest.fixture(scope="module")
def stack(tpl):
    return Stack(tpl)


def one(stack, type_):
    found = stack.resources_of(type_)
    assert len(found) == 1, f"declare exactly one {type_}, found {sorted(found)}"
    return next(iter(found))


def key_policy(stack):
    return stack.value(stack.props(one(stack, "AWS::KMS::Key")).get("KeyPolicy", {}))


def key_request(principal, action, via=None):
    account = iam.account_of(principal)
    context = {"aws:PrincipalArn": principal, "aws:PrincipalAccount": account, "kms:CallerAccount": account}
    if via:
        context["kms:ViaService"] = via
    return iam.Request(principal=principal, action=action, resource=KEY_ARN, context=context)


def key_decision(stack, request):
    # The callers' own IAM policies are taken to allow everything, so the key policy
    # decides: KMS always needs it to allow, and a statement for the account root only
    # lets IAM policies work, under that statement's conditions.
    return iam.evaluate(request, identity=[EVERYTHING], resource_policy=key_policy(stack))


def plan_rules(stack):
    plan = stack.value(stack.props(one(stack, "AWS::Backup::BackupPlan"))["BackupPlan"])
    return {rule["RuleName"]: rule for rule in plan.get("BackupPlanRule", [])}


def vault(stack):
    return stack.value(stack.props(one(stack, "AWS::Backup::BackupVault")))


def lifecycles(stack):
    for name, rule in plan_rules(stack).items():
        yield name, rule.get("Lifecycle", {})
        for copy in rule.get("CopyActions", []):
            yield f"{name} (copy)", copy.get("Lifecycle", {})


# ---- the key --------------------------------------------------------------------------

def test_the_key_is_kept_and_rotates(tpl):
    name, key = next(iter(resources(tpl, "AWS::KMS::Key").items()))
    assert key.get("DeletionPolicy") == "Retain" and key.get("UpdateReplacePolicy") == "Retain", (
        f"deleting the stack must not delete {name}: every recovery point would become unreadable")
    assert key["Properties"].get("EnableKeyRotation") is True
    assert key["Properties"].get("PendingWindowInDays") == 30, "keep the longest waiting period, 30 days"


@pytest.mark.parametrize("action", ["kms:PutKeyPolicy", "kms:EnableKeyRotation", "kms:TagResource"])
def test_the_key_admin_manages_the_key(stack, action):
    assert key_decision(stack, key_request(KEY_ADMIN, action)) == iam.ALLOW


@pytest.mark.parametrize("principal", [KEY_ADMIN, APP, BREAK_GLASS])
@pytest.mark.parametrize("action", ["kms:Decrypt", "kms:GenerateDataKey"])
def test_nobody_uses_the_key_outside_aws_backup(stack, principal, action):
    assert key_decision(stack, key_request(principal, action)) != iam.ALLOW, (
        f"{principal} may call {action} directly: only AWS Backup may use the key (kms:ViaService)")
    through_s3 = key_request(principal, action, via=f"s3.{REGION}.amazonaws.com")
    assert key_decision(stack, through_s3) != iam.ALLOW, f"{principal} may call {action} through S3"


@pytest.mark.parametrize("action", ["kms:Decrypt", "kms:GenerateDataKey", "kms:CreateGrant", "kms:DescribeKey"])
def test_aws_backup_uses_the_key_for_the_account(stack, action):
    assert key_decision(stack, key_request(APP, action, via=BACKUP_VIA)) == iam.ALLOW


def test_the_backup_account_decrypts_only_to_copy(stack):
    assert key_decision(stack, key_request(COPIER, "kms:Decrypt", via=BACKUP_VIA)) == iam.ALLOW, (
        "the backup account must decrypt through AWS Backup to copy the recovery points")
    assert key_decision(stack, key_request(COPIER, "kms:Decrypt")) != iam.ALLOW, "only through AWS Backup"
    assert key_decision(stack, key_request(COPIER, "kms:Encrypt", via=BACKUP_VIA)) != iam.ALLOW
    assert key_decision(stack, key_request(COPIER, "kms:PutKeyPolicy")) != iam.ALLOW
    outsider = f"arn:aws:iam::{OUTSIDER}:role/backup-copier"
    assert key_decision(stack, key_request(outsider, "kms:Decrypt", via=BACKUP_VIA)) != iam.ALLOW


@pytest.mark.parametrize("action", ["kms:ScheduleKeyDeletion", "kms:DisableKey"])
def test_only_break_glass_can_destroy_the_key(stack, action):
    assert key_decision(stack, key_request(KEY_ADMIN, action)) == iam.EXPLICIT_DENY, (
        f"deny {action} to everyone but OrgBreakGlass, with an explicit Deny")
    assert key_decision(stack, key_request(BREAK_GLASS, action)) == iam.ALLOW


# ---- the vault ------------------------------------------------------------------------

def test_the_vault_uses_the_key_and_is_kept(tpl):
    name, found = next(iter(resources(tpl, "AWS::Backup::BackupVault").items()))
    assert found.get("DeletionPolicy") == "Retain", f"set DeletionPolicy: Retain on {name}"
    key = next(iter(resources(tpl, "AWS::KMS::Key")))
    assert getatt(found["Properties"].get("EncryptionKeyArn")) == (key, "Arn"), (
        "encrypt the vault with !GetAtt of your own key")


def test_the_vault_is_locked_in_compliance_mode(stack):
    lock = vault(stack).get("LockConfiguration", {})
    assert lock.get("MinRetentionDays") == 7 and lock.get("MaxRetentionDays") == 365
    assert lock.get("ChangeableForDays") == 3, (
        "ChangeableForDays turns the lock into compliance mode: after 3 days nobody can remove it")


@pytest.mark.parametrize("action", ["backup:DeleteRecoveryPoint", "backup:UpdateRecoveryPointLifecycle",
                                    "backup:PutBackupVaultAccessPolicy", "backup:DeleteBackupVaultAccessPolicy"])
def test_even_an_admin_cannot_delete_recovery_points(stack, action):
    policy = vault(stack).get("AccessPolicy")
    assert policy, "give the vault an AccessPolicy"
    request = iam.Request(principal=ADMIN, action=action, resource=VAULT_ARN)
    assert iam.evaluate(request, identity=[EVERYTHING], resource_policy=policy) == iam.EXPLICIT_DENY


@pytest.mark.parametrize("action", ["backup:StartRestoreJob", "backup:DescribeRecoveryPoint",
                                    "backup:StartBackupJob", "backup:StartCopyJob"])
def test_backups_and_restores_still_work(stack, action):
    request = iam.Request(principal=ADMIN, action=action, resource=VAULT_ARN)
    policy = vault(stack).get("AccessPolicy")
    assert iam.evaluate(request, identity=[EVERYTHING], resource_policy=policy) == iam.ALLOW, (
        f"the vault policy blocks {action}: deny only what destroys recovery points")


# ---- the role -------------------------------------------------------------------------

def test_only_aws_backup_for_this_account_assumes_the_role(stack):
    name = one(stack, "AWS::IAM::Role")
    trust = stack.value(stack.props(name)["AssumeRolePolicyDocument"])

    def assume(principal, source):
        request = iam.Request(principal=principal, action="sts:AssumeRole",
                              resource=stack.attribute(name, "Arn"), context={"aws:SourceAccount": source})
        return iam.evaluate(request, resource_policy=trust)

    assert assume("backup.amazonaws.com", ACCOUNT) == iam.ALLOW
    assert assume("backup.amazonaws.com", OUTSIDER) != iam.ALLOW, (
        "check aws:SourceAccount: without it another account's backup jobs could use the role (confused deputy)")
    assert assume("lambda.amazonaws.com", ACCOUNT) != iam.ALLOW
    assert assume(ADMIN, ACCOUNT) != iam.ALLOW


def test_the_role_can_back_up_and_restore(stack):
    arns = stack.props(one(stack, "AWS::IAM::Role")).get("ManagedPolicyArns", [])
    for suffix in ("ForBackup", "ForRestores"):
        assert f"arn:aws:iam::aws:policy/service-role/AWSBackupServiceRolePolicy{suffix}" in arns, (
            f"attach AWSBackupServiceRolePolicy{suffix}: a backup you cannot restore is no backup")


# ---- the plan -------------------------------------------------------------------------

def test_an_hourly_rule_meets_the_one_hour_rpo(stack):
    hourly = [rule for rule in plan_rules(stack).values()
              if CRON_HOURLY.fullmatch(rule.get("ScheduleExpression", ""))]
    assert hourly, "add a rule whose cron runs every hour (minute fixed, hour *): the RPO is one hour"
    assert all(rule.get("StartWindowMinutes", 480) <= 60 for rule in hourly), (
        "an hourly rule must start within 60 minutes (the default window is 8 hours)")


def test_every_rule_writes_to_the_locked_vault(tpl):
    name = next(iter(resources(tpl, "AWS::Backup::BackupVault")))
    plan = next(iter(resources(tpl, "AWS::Backup::BackupPlan").values()))
    rules = plan["Properties"]["BackupPlan"]["BackupPlanRule"]
    assert len(rules) >= 2, "two rules: hourly for the RPO, daily for the long term"
    for rule in rules:
        assert ref(rule.get("TargetBackupVault")) == name, f"rule {rule.get('RuleName')} must target !Ref {name}"


def test_retention_fits_the_vault_lock(stack):
    lock = vault(stack).get("LockConfiguration", {})
    for name, lifecycle in lifecycles(stack):
        if "(copy)" in name:
            continue
        days = lifecycle.get("DeleteAfterDays")
        assert days is not None, f"rule {name} keeps recovery points forever: set DeleteAfterDays"
        assert lock.get("MinRetentionDays", 0) <= days <= lock.get("MaxRetentionDays", 10**6), (
            f"rule {name} keeps {days} days, outside the vault lock: its backup jobs would fail")


def test_cold_storage_is_used_right(stack):
    cold = [(name, cycle) for name, cycle in lifecycles(stack) if "MoveToColdStorageAfterDays" in cycle]
    assert any("(copy)" not in name for name, _ in cold), (
        "move the long-term recovery points to cold storage: it costs a fraction of warm storage")
    for name, cycle in cold:
        assert cycle.get("DeleteAfterDays", 0) - cycle["MoveToColdStorageAfterDays"] >= 90, (
            f"{name}: cold storage has a 90-day minimum, so DeleteAfterDays must come at least 90 days later")
    longest = max(cycle.get("DeleteAfterDays", 0) for _, cycle in lifecycles(stack))
    assert longest >= 365, "keep the daily recovery points for a year"


def test_dailies_are_copied_to_another_account_and_region(stack):
    copies = [copy for rule in plan_rules(stack).values() for copy in rule.get("CopyActions", [])]
    assert copies, "copy the daily recovery points to the disaster-recovery vault"
    for copy in copies:
        arn = copy.get("DestinationBackupVaultArn", "")
        assert arn == DR_VAULT, f"copy to the DrVaultArn parameter, got {arn!r}"
        assert copy.get("Lifecycle", {}).get("DeleteAfterDays"), "give the copy its own retention"


def test_resources_are_chosen_by_tag(tpl, stack):
    found = stack.value(stack.props(one(stack, "AWS::Backup::BackupSelection"))["BackupSelection"])
    assert not found.get("Resources"), "select by tag, not with a Resources list (`*` backs up everything)"
    wanted = {"ConditionType": "STRINGEQUALS", "ConditionKey": "backup", "ConditionValue": "orders"}
    assert wanted in found.get("ListOfTags", []), "select the resources tagged backup=orders"
    raw = next(iter(resources(tpl, "AWS::Backup::BackupSelection").values()))["Properties"]["BackupSelection"]
    role = next(iter(resources(tpl, "AWS::IAM::Role")))
    assert getatt(raw.get("IamRoleArn")) == (role, "Arn")


# ---- the disaster-recovery vault ------------------------------------------------------

@pytest.fixture(scope="module")
def dr_policy():
    return iam.load("dr-vault-policy.json")


def dr_decision(policy, principal, action, org):
    request = iam.Request(principal=principal, action=action, resource=DR_VAULT,
                          context={"aws:PrincipalOrgID": org, "aws:PrincipalArn": principal})
    return iam.evaluate(request, identity=[EVERYTHING], resource_policy=policy)


def test_the_policy_is_valid(dr_policy):
    assert iam.validate(dr_policy, kind="resource") == []


def test_the_organization_copies_in(dr_policy):
    role = f"arn:aws:iam::{ACCOUNT}:role/service-role/AWSBackupDefaultServiceRole"
    assert dr_decision(dr_policy, role, "backup:CopyIntoBackupVault", ORG) == iam.ALLOW


def test_outsiders_and_other_actions_stay_out(dr_policy):
    outsider = f"arn:aws:iam::{OUTSIDER}:role/x"
    assert dr_decision(dr_policy, outsider, "backup:CopyIntoBackupVault", "o-other0000000") != iam.ALLOW, (
        "only principals of o-acme1234567 may copy in (aws:PrincipalOrgID)")
    assert dr_decision(dr_policy, ADMIN, "backup:StartRestoreJob", ORG) != iam.ALLOW, (
        "the organization may only copy in")


@pytest.mark.parametrize("action", ["backup:DeleteRecoveryPoint", "backup:UpdateRecoveryPointLifecycle",
                                    "backup:PutBackupVaultAccessPolicy"])
def test_the_backup_account_admin_cannot_delete_either(dr_policy, action):
    admin = f"arn:aws:iam::{BACKUP_ACCOUNT}:role/Admin"
    assert dr_decision(dr_policy, admin, action, ORG) == iam.EXPLICIT_DENY
