# Security Services: KMS, Secrets Manager, CloudTrail, Config, GuardDuty and Security Hub

IAM decides who may do what. The security services answer the next questions: how is the
data protected, where are the secrets, who did what, is everything still configured as it
should be, and is anybody attacking us?

## KMS: keys and envelope encryption

**AWS KMS** holds keys in hardware security modules; key material never leaves KMS in
plain text. Services encrypt with **envelope encryption**: KMS generates a **data key**,
the service encrypts your data with it locally, and stores the data key encrypted under
your **KMS key**. Decrypting the data means asking KMS to decrypt the data key, which is
an IAM-checked, CloudTrail-logged call.

| Key | Who manages | Use |
| --- | --- | --- |
| AWS owned | AWS, invisible | the default for many services |
| AWS managed (`aws/s3`, `aws/rds`) | AWS, visible in your account | simple encryption at rest; cannot share across accounts |
| **Customer managed** | you: key policy, rotation, deletion | control and audit who decrypts; cross-account sharing |

Every KMS key has a **key policy** (a resource-based policy, required). Access needs the
key policy to allow it, directly or by delegating to IAM in the account. Turn on
**automatic rotation**; deleting a key is scheduled (7–30 days) because data encrypted
under it becomes unreadable. **Grants** give temporary, narrow permissions to services.
Use the condition `kms:ViaService` to allow decryption only through a given service.

## Secrets

**Secrets Manager** stores database credentials, API keys and tokens, encrypted with KMS,
with **automatic rotation** (Lambda rotation functions, or built in for RDS, Redshift,
DocumentDB), versioning and cross-region replication. Apps read secrets at start-up
through their role (with caching), and CloudFormation reads them through dynamic
references. **SSM Parameter Store** `SecureString` parameters are cheaper and fine for
configuration and static secrets without rotation.

## The audit trail: CloudTrail

**AWS CloudTrail** records API calls: who (principal, role session, source IP), what
(action, parameters), when and the result. Event history keeps 90 days of management
events in each account; a **trail** (best: one **organization trail**) delivers them to an
S3 bucket in the log archive account, with **log file validation** (digests prove nothing
was altered), optionally data events (S3 object reads, Lambda invocations) and CloudTrail
Lake for SQL queries. Protect the trail with SCPs: an attacker's first step is often
`StopLogging`.

## Configuration compliance: AWS Config

**AWS Config** records the configuration history of resources and evaluates **rules**
(managed rules such as `s3-bucket-public-read-prohibited`, `restricted-ssh`,
`rds-storage-encrypted`, or your own, including Guard rules), grouped in **conformance
packs**. Non-compliant resources can trigger **remediation** through Systems Manager
Automation. Config answers "what did this security group look like last Tuesday?".

## Threat detection

| Service | Finds |
| --- | --- |
| **GuardDuty** | threats from CloudTrail, VPC Flow Logs, DNS logs, S3, EKS, RDS and runtime events: crypto-mining, leaked credentials used from odd places, port scans, malware |
| **Inspector** | software vulnerabilities (CVEs) and network exposure in EC2, ECR images and Lambda functions |
| **Macie** | sensitive data (personal data, credentials) in S3 buckets |
| **Detective** | investigates a finding: the graph of what a principal did |
| **IAM Access Analyzer** | resources shared outside the organization, unused access, policy checks |

**Security Hub** aggregates findings from all of them (and partners) across accounts and
regions, scores you against standards (AWS Foundational Security Best Practices, CIS), and
sends findings to EventBridge for automated response. Run all of these organization-wide
with a **delegated administrator** in the security account.

## The edge

- **AWS WAF** filters HTTP requests on CloudFront, ALB, API Gateway and AppSync: managed
  rule groups (OWASP-style common rules, SQL injection, known bad inputs, IP reputation,
  bot control), rate-based rules, geo and IP rules.
- **AWS Shield Standard** (free, automatic) absorbs common network DDoS attacks; **Shield
  Advanced** adds a response team, cost protection and application-layer mitigation.
- **ACM** issues and renews TLS certificates for free for load balancers and CloudFront.

## Preventive, detective, responsive

| Control | Examples |
| --- | --- |
| preventive | SCPs and RCPs, IAM, cfn-guard and cfn-lint in CI, CloudFormation Guard Hooks, Block Public Access |
| detective | Config rules, Security Hub standards, GuardDuty, Access Analyzer |
| responsive | EventBridge rules on findings → Lambda or SSM Automation (quarantine an instance, disable a key), incident runbooks |

You will write preventive rules with cfn-guard in the next kata.

## Summary

- KMS protects data with envelope encryption; customer managed keys give control and audit.
- Secrets Manager stores and rotates secrets; apps read them through their roles.
- CloudTrail records every API call; keep an organization trail in a protected log archive.
- Config tracks configuration and compliance; GuardDuty, Inspector and Macie detect threats.
- Security Hub aggregates findings; WAF, Shield and ACM protect the edge.

## Further reading

- AWS KMS concepts: <https://docs.aws.amazon.com/kms/latest/developerguide/concepts.html>
- Key policies in AWS KMS: <https://docs.aws.amazon.com/kms/latest/developerguide/key-policies.html>
- AWS Secrets Manager user guide: <https://docs.aws.amazon.com/secretsmanager/latest/userguide/intro.html>
- AWS CloudTrail user guide: <https://docs.aws.amazon.com/awscloudtrail/latest/userguide/cloudtrail-user-guide.html>
- AWS Config developer guide: <https://docs.aws.amazon.com/config/latest/developerguide/WhatIsConfig.html>
- Amazon GuardDuty user guide: <https://docs.aws.amazon.com/guardduty/latest/ug/what-is-guardduty.html>
- AWS Security Hub user guide: <https://docs.aws.amazon.com/securityhub/latest/userguide/what-is-securityhub.html>
- AWS CloudFormation Guard: <https://docs.aws.amazon.com/cfn-guard/latest/ug/what-is-guard.html>
