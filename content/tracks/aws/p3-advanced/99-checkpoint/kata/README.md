# Checkpoint 3: Backups That Survive a Bad Day

The orders workload (account `111122223333`, region `eu-west-1`, organization
`o-acme1234567`) has a first draft of its backups in `backup.yaml`. The security review
asks for backups that survive three bad days: an attacker with admin rights in the
account, a deploy that corrupts data, and the loss of the region. Rewrite `backup.yaml`
and `dr-vault-policy.json`.

**`BackupKey`**, the customer managed key of the vault:

| Rule | Detail |
| --- | --- |
| kept and rotated | `DeletionPolicy` and `UpdateReplacePolicy` `Retain`; `EnableKeyRotation: true`; `PendingWindowInDays: 30` |
| key administrators | roles `KeyAdmin` and `OrgBreakGlass` of the account manage the key (`kms:Create*`, `Describe*`, `Enable*`, `List*`, `Put*`, `Update*`, `Revoke*`, `Disable*`, `Get*`, `Delete*`, `TagResource`, `UntagResource`, `ScheduleKeyDeletion`, `CancelKeyDeletion`) but never encrypt or decrypt |
| used only by AWS Backup | the account (`:root`) may `kms:Encrypt`, `Decrypt`, `ReEncrypt*`, `GenerateDataKey*`, `DescribeKey` and `CreateGrant` only when `kms:ViaService` is `backup.<region>.amazonaws.com` and `kms:CallerAccount` is the account |
| copies to the backup account | the backup account (`!Ref BackupAccountId`) may `kms:Decrypt`, `DescribeKey` and `CreateGrant`, also only through AWS Backup |
| hard to destroy | an explicit `Deny` of `kms:ScheduleKeyDeletion` and `kms:DisableKey` to every principal whose `aws:PrincipalArn` is not the `OrgBreakGlass` role |

**`BackupVault`**: `DeletionPolicy: Retain`; encrypted with `!GetAtt BackupKey.Arn`; a
**vault lock** in compliance mode (`MinRetentionDays: 7`, `MaxRetentionDays: 365`,
`ChangeableForDays: 3`); an `AccessPolicy` that denies `backup:DeleteRecoveryPoint`,
`UpdateRecoveryPointLifecycle`, `PutBackupVaultAccessPolicy` and
`DeleteBackupVaultAccessPolicy` to everyone, and nothing else (backups and restores must
keep working).

**`BackupRole`**: only `backup.amazonaws.com` may assume it, and only when
`aws:SourceAccount` is this account; it keeps `AWSBackupServiceRolePolicyForBackup` and
adds `AWSBackupServiceRolePolicyForRestores`.

**`BackupPlan`**, two rules, both into `!Ref BackupVault`:

| Rule | Detail |
| --- | --- |
| hourly | the RPO is one hour: a `cron` with a fixed minute and every hour, `StartWindowMinutes: 60`, kept 7 days |
| daily | kept 365 days, moved to cold storage after 30 days; copied to `!Ref DrVaultArn` (the backup account, `eu-central-1`) with the same lifecycle |

Every retention must fit the vault lock, and cold storage keeps a recovery point at least
90 days, so `DeleteAfterDays` must come 90 days or more after `MoveToColdStorageAfterDays`.

**`BackupSelection`**: the resources tagged `backup` = `orders` (`ListOfTags`, condition
`STRINGEQUALS`), not a `Resources` list, with `!GetAtt BackupRole.Arn`.

**`dr-vault-policy.json`**, the access policy of the `acme-dr` vault in the backup account
`999900001111`: any principal of the organization (`aws:PrincipalOrgID`) may
`backup:CopyIntoBackupVault`, nothing else is allowed, and the same four destructive
actions are denied to everyone, the backup account's own admins included.

How the check works: cfn-lint validates every resource (it knows the cold storage rule);
the tests resolve the template as a deployed stack and send requests to the dojo's IAM
evaluator. Key policy requests are decided by the key policy alone, as KMS does in the
key's account: the callers' own IAM policies are taken to allow everything.

With an AWS account (not needed here):

```powershell
aws cloudformation deploy --template-file backup.yaml --stack-name orders-backups --capabilities CAPABILITY_IAM
aws backup start-backup-job --backup-vault-name orders-vault --resource-arn <table arn> --iam-role-arn <role arn>
aws backup delete-recovery-point --backup-vault-name orders-vault --recovery-point-arn <arn>   # AccessDenied
```

There are 43 tests: 1 in `tests/test_visible.py`, 41 hidden ones and the cfn-lint check.
