# Organization Guardrails

Rewrite the four policies. Organization `o-acme1234567`; development account
`111122223333`, analytics account `444455556666`, data account `777788889999`.

**`scp-guardrails.json`**, an SCP on the workloads OU (next to `FullAWSAccess`):

| Rule | Detail |
| --- | --- |
| EU regions only | deny every action outside `eu-west-1` and `eu-central-1` (`aws:RequestedRegion`)... |
| ...except global services | `iam`, `sts`, `organizations`, `cloudfront`, `route53`, `support` (and billing: `budgets`, `ce`, `health`) |
| ...and the break-glass role | any role named `OrgAdmin` (`aws:PrincipalArn` like `arn:aws:iam::*:role/OrgAdmin`) |
| protect the audit trail | deny `cloudtrail:StopLogging`, `DeleteTrail`, `UpdateTrail`, `PutEventSelectors` to everyone |
| stay in the organization | deny `organizations:LeaveOrganization` to everyone |

**`developer-policy.json`**, the developers' identity policy (keep `BuildApps`):

| Rule | Detail |
| --- | --- |
| create app roles | `iam:CreateRole`, `PutRolePolicy`, `AttachRolePolicy` on `arn:aws:iam::111122223333:role/app/*`, only when `iam:PermissionsBoundary` is `arn:aws:iam::111122223333:policy/app-boundary` |
| pass app roles | `iam:PassRole` on `role/app/*` only |
| never loosen the boundary | explicitly deny `iam:DeleteRolePermissionsBoundary`, `PutRolePermissionsBoundary`, `CreatePolicyVersion`, `DeletePolicy`, `SetDefaultPolicyVersion` |
| nothing else in IAM | no users, access keys or other roles |

**`app-boundary.json`**, the permissions boundary of every app role: allow only the exact
actions apps use (no wildcards): `s3:GetObject`, `PutObject`, `ListBucket`;
`dynamodb:GetItem`, `PutItem`, `UpdateItem`, `Query`; `sqs:SendMessage`, `ReceiveMessage`,
`DeleteMessage`, `GetQueueAttributes`; `logs:CreateLogStream`, `PutLogEvents`;
`xray:PutTraceSegments`.

**`bucket-policy.json`**, on `acme-shared-datasets` in the data account:

| Rule | Detail |
| --- | --- |
| analytics reads | `s3:GetObject` and `s3:ListBucket` for `arn:aws:iam::444455556666:role/analytics` (the bucket and its objects) |
| organization only | deny `s3:*` to any principal whose `aws:PrincipalOrgID` is not `o-acme1234567` |
| TLS only | deny `s3:*` when `aws:SecureTransport` is `false` |

How the check works: the tests evaluate requests with the dojo's evaluator, layering the
policies as AWS does: SCPs per organization level, boundaries as a cap, explicit denies
first, and both sides for cross-account calls.

With an AWS account (not needed here):

```powershell
aws accessanalyzer validate-policy --policy-type SERVICE_CONTROL_POLICY --policy-document file://scp-guardrails.json
aws accessanalyzer validate-policy --policy-type RESOURCE_POLICY --policy-document file://bucket-policy.json
```

There are 54 tests: 1 in `tests/test_visible.py` and 53 hidden ones.
