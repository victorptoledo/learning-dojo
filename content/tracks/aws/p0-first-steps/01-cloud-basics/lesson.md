# The Cloud, the Shared Responsibility Model, Regions and Accounts

Amazon Web Services (AWS) rents you computers, storage, networks and more than two hundred
managed services, by the second or by the request, through an API. This track teaches the
pieces a backend developer and an architect need, and every kata is checked **offline**:
you write real CloudFormation templates, IAM policies and boto3 code, and the dojo checks
them with the tools teams use (cfn-lint, cfn-guard) plus a policy evaluator and moto, an
in-memory AWS. You never need an AWS account, a credit card or credentials.

## What the cloud changes

| Before | On AWS |
| --- | --- |
| Buy servers months ahead, sized for the peak | Start capacity in minutes, stop it when idle |
| Pay up front (capex) | Pay for what you use (opex), with discounts for commitment |
| Run your own database, queue, load balancer | Use a managed service (RDS, SQS, ELB) |
| A ticket to the network team | An API call, or a line in a template |

The last row is the important one: **everything is an API**. The console, the `aws` CLI,
the SDKs (boto3, the AWS SDKs for Java, JavaScript, .NET and Go) and CloudFormation all call
the same HTTPS endpoints, signed with your credentials (Signature Version 4). If you can
click it, you can script it, and you should: Phase 1 ends with infrastructure as code.

## The shared responsibility model

Security is shared. **AWS is responsible for the security *of* the cloud**: data centres,
hardware, the hypervisor, the global network, and the software of the managed services.
**You are responsible for security *in* the cloud**: your data, who can reach it, how it
is encrypted, and whatever you install. Where the line falls depends on the service:

| You manage | EC2 (IaaS) | RDS (managed) | Lambda, S3, DynamoDB (serverless) |
| --- | --- | --- | --- |
| Data, classification, encryption choices | ✔ | ✔ | ✔ |
| IAM: who may call what | ✔ | ✔ | ✔ |
| Network exposure (security groups, public access) | ✔ | ✔ | ✔ (bucket policies, function URLs) |
| Application code and its dependencies | ✔ | ✔ | ✔ |
| Guest operating system, its patches and agents | ✔ | AWS | AWS |
| Database engine install and patches | ✔ (if you run one) | AWS (you pick the window) | AWS |
| Capacity, scaling, the runtime | ✔ (Auto Scaling) | partly (instance size) | AWS |

Most cloud breaches are on the customer side of that line: a public S3 bucket, an access
key committed to Git, an admin policy on a CI user, an SSH port open to the world. That is
why IAM is the first topic of Phase 1.

## Regions, Availability Zones and edge locations

A **region** (`eu-west-1` Ireland, `us-east-1` N. Virginia, `sa-east-1` São Paulo) is an
independent geographic area. Most resources live in one region, and data stays there
unless you move it. A region has three or more **Availability Zones** (AZs, such as
`eu-west-1a`): one or more data centres each, with separate power and networking, linked by
low-latency fibre. Spreading across AZs is how you survive a data-centre failure.
**Edge locations** (hundreds of them) serve CloudFront and Route 53 close to users.

Choose a region by, in order: **law and data residency** (GDPR, LGPD), **latency** to
users, **service and feature availability** (new services start in a few regions), and
**price** (the same instance can cost 30–50 % more in São Paulo than in N. Virginia). A few
services are **global**: IAM, Route 53, CloudFront, Organizations.

## Accounts and the root user

An **AWS account** is the unit of billing and the hard isolation boundary: resources in two
accounts cannot see each other unless a policy says so. Real organizations use many
accounts (one per environment and team) under **AWS Organizations**; Phase 3 covers that.

The email address that opened the account is the **root user**. It can do anything,
including closing the account, and no IAM policy limits it inside its own account. So:

1. Turn on MFA for the root user, keep no access keys for it, and lock the credentials away.
2. Give people access through **IAM Identity Center** (single sign-on, short-lived
   credentials) or, in a single small account, IAM users in groups with MFA; never share
   logins.
3. Create an **AWS Budgets** alert (for example at 10 USD) on the first day, and read the
   Free Tier limits: an EC2 instance or a NAT gateway left running is a classic first bill.
4. Make sure **CloudTrail** keeps a trail of every API call (Phase 3) and pick your region.

## Talking to AWS

```powershell
aws configure sso                 # sign in through IAM Identity Center once
aws sts get-caller-identity       # who am I? the first command to run when anything fails
aws s3 ls --profile dojo-dev      # every command takes a profile and a --region
```

Every SDK finds credentials the same way, the **default credential provider chain**:
environment variables, the shared config and SSO cache in `~/.aws`, then the role of the
machine it runs on (an EC2 instance profile, an ECS task role, a Lambda execution role).
Code in AWS should never hold access keys: it gets temporary credentials from its role.

## How the dojo checks AWS work

| Kata kind | Checked by |
| --- | --- |
| CloudFormation and SAM templates | cfn-lint (the AWS schemas of every resource type), cfn-guard rules and pytest tests over the parsed template |
| IAM policies | the dojo's policy evaluator: a matrix of requests that must be allowed or denied |
| Python code that calls AWS | pytest with **moto**, which simulates S3, DynamoDB, SQS and more in memory |

The next node is your first template: a private, encrypted S3 bucket.

## Summary

- AWS rents capacity and managed services through APIs; you pay for what you use.
- AWS secures the cloud; you secure what you put in it, and the line moves with the service.
- Regions are independent; AZs inside them are isolated data centres; spread across AZs.
- Protect the root user, give people SSO or IAM access, set a budget alert on day one.
- The CLI and SDKs use the credential provider chain; code in AWS uses roles, not keys.

## Further reading

- Shared responsibility model: <https://aws.amazon.com/compliance/shared-responsibility-model/>
- Regions and Availability Zones: <https://docs.aws.amazon.com/global-infrastructure/latest/regions/aws-regions.html>
- Root user best practices: <https://docs.aws.amazon.com/IAM/latest/UserGuide/root-user-best-practices.html>
- AWS CLI, getting started: <https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html>
- Credential provider chain (SDKs and Tools Reference Guide): <https://docs.aws.amazon.com/sdkref/latest/guide/standardized-credentials.html>
- AWS Budgets: <https://docs.aws.amazon.com/cost-management/latest/userguide/budgets-managing-costs.html>
