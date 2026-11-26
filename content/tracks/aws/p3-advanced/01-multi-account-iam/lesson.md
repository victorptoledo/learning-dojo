# Multi-Account IAM: Organizations, SCPs, Boundaries and Cross-Account Access

An AWS account is the strongest isolation boundary AWS offers: separate resources,
separate IAM, separate quotas and a separate bill. Real organizations therefore use
**many accounts**: one per environment, per team or per workload. A mistake in a sandbox
cannot touch production when production is another account.

## AWS Organizations

**AWS Organizations** groups accounts under one **management account** (billing, the
organization itself; keep workloads out of it) into a tree of **organizational units**:

```text
Root
├── Security OU     → security tooling account, log archive account
├── Infrastructure  → networking (Transit Gateway), shared services, CI/CD tooling
├── Workloads OU
│   ├── Prod OU     → orders-prod, payments-prod
│   └── NonProd OU  → orders-dev, orders-staging
└── Sandbox OU      → one account per developer, with a budget
```

**AWS Control Tower** builds this **landing zone** for you: the OUs, the log archive and
audit accounts, an organization CloudTrail trail, AWS Config, and a catalogue of
**controls** (preventive SCPs, detective Config rules, proactive Guard hooks), plus
Account Factory to create new accounts the same way every time.

## Service and resource control policies

A **service control policy** (SCP) attached to the root, an OU or an account sets the
**maximum** permissions of every principal in the accounts below it, including each
account's root user. SCPs **grant nothing**: a request must be allowed by IAM *and* not be
blocked by any SCP on the path. AWS attaches `FullAWSAccess` everywhere by default, so
SCPs are usually written as **deny-lists**:

```json
{
  "Sid": "DenyOutsideEuRegions",
  "Effect": "Deny",
  "NotAction": ["iam:*", "sts:*", "organizations:*", "cloudfront:*", "route53:*", "support:*"],
  "Resource": "*",
  "Condition": {
    "StringNotEquals": { "aws:RequestedRegion": ["eu-west-1", "eu-central-1"] },
    "ArnNotLike": { "aws:PrincipalArn": "arn:aws:iam::*:role/OrgAdmin" }
  }
}
```

Typical guardrails: allowed regions (with global services exempted), never stop CloudTrail,
Config or GuardDuty, never leave the organization, deny the root user, require IMDSv2 or
encryption. SCPs do **not** apply to the management account or to service-linked roles.

**Resource control policies** (RCPs) are their mirror image: maximum permissions on
**resources** (S3, KMS, SQS, Secrets Manager, STS) in the organization, whoever calls. A
classic RCP denies access to every bucket from principals outside the organization
(`aws:PrincipalOrgID`), building a **data perimeter**.

## People: IAM Identity Center

**IAM Identity Center** connects your identity provider (Entra ID, Okta, Google, or its
own directory) to every account. A **permission set** (for example `ReadOnly` or
`DeveloperPowerUser`) becomes a role in each account it is assigned to; people pick an
account and role in the portal or with `aws sso login` and get credentials for a few
hours. No IAM users, no long-lived keys, and access is granted by group membership.

## Delegation: permissions boundaries

Developers often need to create roles for their own Lambda functions. Letting them call
`iam:CreateRole` freely lets them create an admin role and assume it. A **permissions
boundary** is a managed policy that caps what an identity can ever do; effective
permissions are the **intersection** of its identity policies and its boundary. The
pattern:

1. Write a boundary that allows only what app roles may do.
2. Let developers create roles **only** with that boundary attached:
   `"Condition": { "StringEquals": { "iam:PermissionsBoundary": "arn:aws:iam::111122223333:policy/app-boundary" } }`.
3. Explicitly deny changing or removing the boundary, and limit `iam:PassRole` to the app
   role path.

## Cross-account access

Two ways for account B to use something in account A:

- **Assume a role in A**: A's role has a trust policy naming B's principal (or account),
  B's principal has `sts:AssumeRole` on it. Best for broad access (deployments, admin).
  Third parties use an **external id** condition against the confused deputy.
- **A resource-based policy in A** (bucket, queue, key, topic, secret) naming B's
  principal. Best for one resource; the caller keeps its own identity.

Either way, a cross-account request needs **both sides** to allow: B's identity policy and
A's resource or trust policy. Use `aws:PrincipalOrgID` and `aws:SourceOrgID` conditions to
allow "anyone in our organization" without listing accounts. **IAM Access Analyzer**
reports every resource shared outside your organization.

## Attribute-based access control

Instead of one policy per team, **ABAC** compares tags: allow `dynamodb:*` when
`aws:ResourceTag/team` equals `${aws:PrincipalTag/team}`. Session tags from Identity
Center or the IdP carry the person's attributes. Fewer policies, but tags themselves must
be protected (who may change them is now a security question).

## The complete evaluation logic

1. Gather every applicable policy: SCPs and RCPs on the path, resource-based policy,
   identity policies, permissions boundary, session policies.
2. **Any explicit deny** in any of them → deny.
3. **Every SCP level and every RCP level** must allow (an implicit deny there is final).
4. A **boundary**, if present, must allow; **session policies**, if present, must allow.
5. Same account: an allow in the identity policy **or** the resource policy → allow.
   A resource policy that names only the **account** (`arn:aws:iam::<account>:root` or
   the account id) delegates to it: the identity policy must allow too. One that names the
   role or user ARN is enough by itself. Cross-account: both → allow. (KMS is stricter:
   the key policy must always allow, and the account principal is what lets IAM policies
   work at all.)
6. Otherwise → implicit deny.

## Summary

- Use many accounts under Organizations; keep the management account empty; Control Tower sets it up.
- SCPs and RCPs set maximums and grant nothing; write them as deny-lists next to FullAWSAccess.
- People use IAM Identity Center permission sets, not IAM users.
- Permissions boundaries make delegated role creation safe.
- Cross-account access needs both sides; use roles for broad access, resource policies for one resource.

## Further reading

- AWS Organizations user guide: <https://docs.aws.amazon.com/organizations/latest/userguide/orgs_introduction.html>
- Service control policies: <https://docs.aws.amazon.com/organizations/latest/userguide/orgs_manage_policies_scps.html>
- Resource control policies: <https://docs.aws.amazon.com/organizations/latest/userguide/orgs_manage_policies_rcps.html>
- Permissions boundaries for IAM entities: <https://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies_boundaries.html>
- Cross-account policy evaluation logic: <https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_evaluation-logic-cross-account.html>
- IAM Identity Center user guide: <https://docs.aws.amazon.com/singlesignon/latest/userguide/what-is.html>
- Organizing your AWS environment using multiple accounts (whitepaper): <https://docs.aws.amazon.com/whitepapers/latest/organizing-your-aws-environment/organizing-your-aws-environment.html>
