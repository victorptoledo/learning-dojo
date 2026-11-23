The audit is passing because you read the README carefully. The next team will not. Write
the company's rules as code, in `rules/company.guard` (this stage added it, with a worked
example), so every stack in the company is checked by `cfn-guard` before it deploys. Keep
these seven rule names:

| Rule | A stack passes when |
| --- | --- |
| `imdsv2_only` | every launch template sets `MetadataOptions.HttpTokens: required` (no `MetadataOptions` at all means IMDSv1 is on) |
| `encrypted_volumes` | every block device mapping of a launch template that has an `Ebs` volume has `Ebs.Encrypted: true` (instance store mappings have no `Ebs`) |
| `resilient_databases` | every RDS instance is `MultiAZ`, keeps backups at least 7 days, has deletion protection, and is encrypted with a customer managed key (`StorageEncrypted` and a `KmsKeyId`) |
| `no_database_passwords` | no RDS instance has a `MasterUserPassword`, and each has `ManageMasterUserPassword: true` |
| `rotating_keys` | every KMS key rotates (done for you) |
| `https_only_listeners` | every HTTPS listener has a TLS 1.3 policy (`ELBSecurityPolicy-TLS13-...`), and every action of an HTTP listener is a redirect to HTTPS |
| `scoped_role_policies` | no **Allow** statement of a role's inline policies has a `*` anywhere in an `Action` (`*`, `s3:*`, `logs:Put*`) or a `Resource` of `*`. Deny statements may use wildcards: a deny of everything without TLS is a good thing |

A rule must **skip** (not fail) a stack that has none of its resources, and every clause
needs a `<<message>>` so the team whose deployment is refused knows what to fix.

The tests run your rules on your own `app.yaml` (it must pass), on a compliant stack, and on
fixtures that each make one mistake: each must break **exactly** its rule and no other. An
`Action` may be a single string or a list, and a `Resource` too.

> [!TIP]
> `%roles.Properties.Policies[*].PolicyDocument.Statement[ Effect == 'Allow' ]` keeps only
> the Allow statements. `Action[*] != /\*/` checks every action against a regular
> expression, whether `Action` is a string or a list. A `Resource` may also be an intrinsic
> function (`!GetAtt Logs.Arn` is a map), which never equals a string, so `Resource[*] != '*'`
> fails on it: check a string `Resource` with `when Resource is_string { ... }` and a list
> with a filter, `Resource[ this == '*' ] empty`.
