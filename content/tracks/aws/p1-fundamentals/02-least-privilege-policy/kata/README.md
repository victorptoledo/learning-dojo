# Least Privilege: Tighten a Role's Policies

The role `arn:aws:iam::111122223333:role/invoice-service` (account `111122223333`, region
`eu-west-1`) runs the invoice service on ECS. Rewrite its two policies.

`policy.json`, the permissions policy, must allow exactly this and nothing more:

| The service... | Action(s) | Resource |
| --- | --- | --- |
| reads and writes invoices | `s3:GetObject`, `s3:PutObject` | objects under `invoices/` in the bucket `acme-invoices` |
| lists invoices | `s3:ListBucket` | the bucket `acme-invoices`, only when `s3:prefix` starts with `invoices/` |
| keeps invoice records | `dynamodb:GetItem`, `PutItem`, `UpdateItem`, `Query` | the table `Invoices` and its indexes |
| announces new invoices | `sqs:SendMessage` | the queue `invoice-events` |

Also:

- no wildcard in any allowed action (`s3:Get*`, `sqs:*`), and no `Allow` with `NotAction`;
- an explicit **Deny** of S3, DynamoDB and SQS actions when `aws:SecureTransport` is
  `false`, so no request without TLS ever succeeds.

`trust-policy.json`, the trust policy, must let only ECS tasks of this account assume
the role: the service principal `ecs-tasks.amazonaws.com`, with `aws:SourceAccount` equal
to `111122223333` and `aws:SourceArn` matching `arn:aws:ecs:eu-west-1:111122223333:*`.
Users, other accounts and other services must be refused, even if their own policies
allow them everything.

How the check works: both files must be valid JSON policies (`Version` `2012-10-17`,
`Effect`, `Action`, `Resource`, and a `Principal` in the trust policy). The tests send
requests to the dojo's evaluator (explicit deny, then allow, otherwise implicit deny) and
compare the decision.

With an AWS account (not needed here):

```powershell
aws accessanalyzer validate-policy --policy-type IDENTITY_POLICY --policy-document file://policy.json
aws iam simulate-custom-policy --policy-input-list file://policy.json `
    --action-names s3:DeleteObject --resource-arns arn:aws:s3:::acme-invoices/invoices/1.pdf
```

There are 32 tests: 1 in `tests/test_visible.py` and 31 hidden ones.
