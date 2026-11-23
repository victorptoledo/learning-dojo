Now the data. Add the parameters `DataSubnetIds` (`List<AWS::EC2::Subnet::Id>`, the three
isolated subnets) and `DatabaseClass` (`AllowedValues` `db.t4g.medium` and `db.m7g.large`,
default `db.t4g.medium`).

| Resource | Rule |
| --- | --- |
| `DataKey` (`AWS::KMS::Key`) | a customer managed key with **automatic rotation**, a 30-day deletion window, and a key policy that lets this account's IAM administer it (`arn:aws:iam::${AWS::AccountId}:root` with `kms:*`) and no one else; `DeletionPolicy` and `UpdateReplacePolicy` `Retain` (deleting the key would make every snapshot unreadable) |
| `DataKeyAlias` | `alias/${AWS::StackName}-data` for the key |
| `DatabaseSecurityGroup` | tcp 5432 from `AppSecurityGroup` only |
| `DatabaseSubnetGroup` | the data subnets |
| `Database` (`AWS::RDS::DBInstance`) | `postgres` 17 (a version cfn-lint does not flag as deprecated), the class from the parameter, 50 GiB of `gp3`; **`MultiAZ: true`**; encrypted with `DataKey`; not publicly accessible; deletion protection; backups kept 14 days; tags copied to snapshots; minor versions upgraded automatically; `DeletionPolicy` and `UpdateReplacePolicy` `Snapshot` |
| the password | nowhere in the template or its parameters: `ManageMasterUserPassword: true` makes RDS create the password in Secrets Manager and rotate it; encrypt that secret with `DataKey` too (`MasterUserSecret: { KmsKeyId: ... }`) |

The tests read the database's properties and run the key policy through the IAM evaluator:
an administrator of this account, whose own policies allow KMS, may manage and use the key;
a principal of another account may not, whatever its own policies say.

> [!TIP]
> In a key policy, `"Principal": {"AWS": "arn:aws:iam::111122223333:root"}` does not mean
> the root user: it means *the account*, so IAM policies in the account decide who among its
> principals may use the key. Without that statement, not even the account's administrators
> could manage the key.
