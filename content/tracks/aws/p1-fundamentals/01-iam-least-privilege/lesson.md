# IAM: Roles, Policies and Least Privilege

Every call to AWS is signed by a **principal** and checked by **IAM** (Identity and Access
Management) against **policies**. Get IAM right and most other mistakes stay small; get it
wrong and a leaked key becomes the whole account. IAM is global (not per region) and free.

## Principals: users, groups and roles

| Identity | Credentials | Use it for |
| --- | --- | --- |
| **Root user** | email + password | almost nothing (P0) |
| **IAM user** | password and/or long-lived access keys | rare today: break-glass access, a legacy tool that cannot assume roles |
| **IAM group** | none (a set of users) | attaching policies to users by job |
| **IAM role** | **temporary** credentials from STS, which expire | everything else: people (through IAM Identity Center), EC2, Lambda, ECS tasks, CI pipelines, other accounts |

A **role** has two policies: a **trust policy** (who may assume it) and **permissions
policies** (what it may do once assumed). Assuming a role (`sts:AssumeRole`, or
`AssumeRoleWithWebIdentity` for GitHub Actions or GitLab OIDC tokens) returns an access
key, a secret and a session token that expire after 15 minutes to 12 hours. Nothing to
rotate, nothing to leak for long.

## The policy document

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ReadReports",
      "Effect": "Allow",
      "Action": ["s3:GetObject"],
      "Resource": "arn:aws:s3:::acme-reports/${aws:PrincipalTag/team}/*",
      "Condition": { "Bool": { "aws:SecureTransport": "true" } }
    }
  ]
}
```

- **Version** is always `2012-10-17` (the older one has no policy variables).
- **Effect** is `Allow` or `Deny`.
- **Action** names API operations as `service:Operation`; wildcards (`s3:Get*`) are
  allowed but widen access. **NotAction** means "every action except these".
- **Resource** is a list of ARNs: `arn:partition:service:region:account:resource`. S3
  ARNs have no region or account; a bucket (`arn:aws:s3:::acme-reports`) and its objects
  (`arn:aws:s3:::acme-reports/*`) are different resources, and `s3:ListBucket` acts on
  the bucket while `s3:GetObject` acts on objects.
- **Condition** adds tests on the request context: `StringEquals`, `StringLike`,
  `ArnLike`, `IpAddress`, `Bool`, `NumericLessThan`, `DateGreaterThan`, `Null`, with
  `...IfExists` and the set prefixes `ForAnyValue:` and `ForAllValues:`. Keys like
  `aws:SourceIp`, `aws:PrincipalTag/team`, `aws:RequestedRegion`, `s3:prefix`.
  Several keys or operators are ANDed; several values for one key are ORed.
- **Policy variables** such as `${aws:username}` or `${aws:PrincipalTag/team}` are filled
  from the request, so one policy can serve many users.

Policies come as **identity-based** (attached to a user, group or role: AWS managed,
customer managed, or inline) and **resource-based** (on the resource itself: S3 bucket
policies, SQS queue policies, KMS key policies, and a role's trust policy). Resource-based
policies have a **Principal**; identity-based ones do not.

## How a request is evaluated

For a request inside one account:

1. Start from **deny** (the implicit deny).
2. If any applicable statement says **Deny**, the answer is deny. An explicit deny always
   wins, whatever allows exist.
3. Otherwise, if an identity-based or resource-based statement says **Allow**, the answer
   is allow.
4. Otherwise it stays denied (implicit deny).

Permissions boundaries, SCPs, session policies and cross-account rules add more gates;
Phase 3 completes the picture. The IAM policy simulator and the dojo's evaluator answer
"would this request be allowed?" without making it.

## Least privilege

Grant only what a workload needs, on the resources it needs, under the conditions it needs:

- **Scope actions**: `dynamodb:GetItem` and `dynamodb:Query`, not `dynamodb:*`.
- **Scope resources**: the table ARN (and `/index/*` for its indexes), not `*`.
- **Use conditions**: require TLS (`aws:SecureTransport`), a VPC endpoint
  (`aws:SourceVpce`), a tag (`aws:ResourceTag/env`), or MFA for sensitive actions.
- **Prefer roles to keys** everywhere; if a key must exist, rotate it and alert on age.
- **Separate duties**: one role per service or function, not one shared "app" role.
- **Start small and grow** from errors, or generate a policy from CloudTrail activity with
  **IAM Access Analyzer**, which also validates policies, finds resources shared outside
  your account and reports unused permissions.

A common mistake is a deny that is too wide: `"Effect": "Deny", "NotAction": "s3:Get*"`
also denies every other service. Read every `Not*` element twice.

## Summary

- Principals are users, groups and roles; workloads and people should use roles.
- A role has a trust policy (who) and permissions policies (what).
- Statements combine Effect, Action, Resource and Condition; ARNs name the resources.
- Explicit deny beats allow; anything not allowed is implicitly denied.
- Least privilege: narrow actions, exact ARNs, conditions, and no long-lived keys.

## Further reading

- IAM policy elements reference: <https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements.html>
- Policy evaluation logic: <https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_evaluation-logic.html>
- IAM roles: <https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles.html>
- Security best practices in IAM: <https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html>
- Condition operators: <https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html>
- IAM Access Analyzer: <https://docs.aws.amazon.com/IAM/latest/UserGuide/what-is-access-analyzer.html>
