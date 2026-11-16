# Policy as Code: Security Rules in cfn-guard

Finish `security.guard`: five rules the platform team runs on every template. The first
rule is half written as an example. Keep the five rule names.

| Rule | A template passes when... |
| --- | --- |
| `s3_buckets_are_private_and_encrypted` | every `AWS::S3::Bucket` sets the four `PublicAccessBlockConfiguration` settings to `true` **and** every rule of `BucketEncryption.ServerSideEncryptionConfiguration` has an `SSEAlgorithm` of `AES256`, `aws:kms` or `aws:kms:dsse` |
| `no_ssh_or_rdp_from_the_internet` | no `AWS::EC2::SecurityGroup` ingress rule from `0.0.0.0/0` or `::/0` allows all traffic (`IpProtocol: "-1"`) or has a port range that includes 22 or 3389 |
| `databases_are_encrypted_private_and_protected` | every `AWS::RDS::DBInstance` has `StorageEncrypted: true`, `DeletionProtection: true` and is not `PubliclyAccessible` (missing or `false`) |
| `tables_have_point_in_time_recovery` | every `AWS::DynamoDB::Table` has `PointInTimeRecoveryEnabled: true` |
| `roles_grant_no_wildcard_admin` | no `Allow` statement in an `AWS::IAM::Role`'s inline `Policies` has the action `"*"`, written as a string or inside a list |

Each rule must **skip** templates without its resource type (`when %x !empty`), and every
clause needs a `<<message>>` so a team that fails knows what to fix.

`tests/fixtures/` holds a compliant template and five broken ones; each broken one must
break exactly its rule. The hidden tests use six more broken variants (a port range
around 3389, all traffic open, a public database, missing deletion protection, an
unencrypted bucket, `"*"` inside a list) and a template with none of these resources.

Try it yourself (with cfn-guard installed, or in the image):

```powershell
cfn-guard validate --rules security.guard --data tests/fixtures --show-summary fail
```

How the check works: `cfn-guard parse-tree` must accept the file, then pytest runs
`cfn-guard validate` on every fixture and compares the failed rules.

There are 15 tests: 6 in `tests/test_visible.py` and 9 hidden ones.
