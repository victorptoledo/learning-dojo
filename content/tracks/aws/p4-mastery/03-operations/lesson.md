# Running AWS in Production: Landing Zones, Deployments and Incidents

Building a workload is the start. Mastery is running dozens of them, changing them every
day, and keeping them healthy when things go wrong: the operational excellence pillar in
practice.

## The landing zone

A production organization usually has:

- **Control Tower** with the OUs from Phase 3, preventive and detective controls, and
  **Account Factory** (or Account Factory for Terraform) to vend new accounts with the
  baseline already in place: SSO access, logging, a VPC from IPAM, budgets, security tools.
- **Networking** in a dedicated account: Transit Gateway or Cloud WAN between VPCs, shared
  VPC endpoints, centralised egress and inspection, Route 53 Resolver for hybrid DNS.
- **Logging and security** accounts: the organization CloudTrail trail, Config, GuardDuty,
  Security Hub and IAM Access Analyzer with delegated administrators.
- **Guardrails as code**: SCPs, RCPs, Config conformance packs and Guard hooks in Git,
  deployed by pipeline like any other change.

## Deploying safely

Most incidents follow a change. So make changes small, frequent, observable and
reversible:

- **One pipeline, many accounts**: build once in a tooling account, then deploy the same
  artifact and templates to dev, staging and production, each through a role in the target
  account (CodePipeline, GitHub Actions with OIDC, or GitLab).
- **Progressive delivery**: Lambda aliases with CodeDeploy `Canary10Percent5Minutes` or
  `Linear10PercentEvery1Minute`; ECS blue/green with test traffic and bake time; weighted
  target groups or Route 53 weights; feature flags with **AppConfig**.
- **Automatic rollback**: alarms (error rate, latency) attached to the deployment, ECS's
  circuit breaker, CloudFormation rollback triggers. A human should not have to notice.
- **Wave by wave**: one region or one cell at a time, the smallest blast radius first.
  Large systems use **cell-based architecture** so a bad change hits a fraction of users.

## Operating fleets

**AWS Systems Manager** is the operations toolbox: **Session Manager** (shell without SSH),
**Run Command** and **Automation runbooks** (repeatable, audited operations such as
"restart service", "patch and reboot", "rotate a key"), **Patch Manager** with maintenance
windows, **Parameter Store**, **Inventory**, and **OpsCenter** for operational issues.

**Service quotas** are real limits (Lambda concurrency, VPCs per region, EIPs, API rates):
monitor usage against them with CloudWatch and raise them **before** a launch, not during.
**AWS Health** reports service events and scheduled maintenance that affect your
resources; send its events to EventBridge and on to your on-call tool.

## Incidents

When the pager goes off:

1. **Declare** the incident and assign roles: an incident commander, operators, a
   communications lead. **Systems Manager Incident Manager** can open the incident from an
   alarm, page responders, run a runbook and keep a timeline.
2. **Mitigate first**: roll back the last change, shift traffic away from a bad AZ or
   region, scale out, shed load or turn off a feature. Root cause can wait; customers
   cannot.
3. **Diagnose with signals**: alarms and dashboards, deployment history, CloudWatch Logs
   Insights, X-Ray traces, CloudTrail for recent changes, AWS Health for provider issues.
   Open an AWS Support case early when a service seems at fault.
4. **Communicate** on a fixed cadence (every 15–30 minutes), internally and on a status page.
5. **Review** afterwards, **blamelessly**: impact, timeline, what made it possible, what
   made it worse, what helped, and action items with owners and dates. Amazon calls this a
   Correction of Errors (COE). Track the actions to done.

## Practising failure

**Game days** rehearse incidents on purpose. **AWS Fault Injection Service** runs
experiments with stop conditions (AZ power interruption scenarios, API throttling, task or
instance termination, network disruption) so teams practise runbooks and find weak spots
in a controlled way. **AWS Resilience Hub** scores workloads against their RTO/RPO and
recommends fixes.

## Summary

- A landing zone gives every new account the same guardrails, access, logging and network.
- Deploy small changes progressively, with alarms that roll back by themselves.
- Systems Manager runs fleet operations; watch quotas and AWS Health before they hurt.
- In an incident: declare, mitigate first, diagnose with signals, communicate, review blamelessly.
- Practise with game days and Fault Injection Service; failure you rehearsed is smaller.

## Further reading

- AWS Control Tower user guide: <https://docs.aws.amazon.com/controltower/latest/userguide/what-is-control-tower.html>
- Deployment strategies (Introduction to DevOps on AWS): <https://docs.aws.amazon.com/whitepapers/latest/introduction-devops-aws/deployment-strategies.html>
- AWS Systems Manager user guide: <https://docs.aws.amazon.com/systems-manager/latest/userguide/what-is-systems-manager.html>
- Service Quotas user guide: <https://docs.aws.amazon.com/servicequotas/latest/userguide/intro.html>
- AWS Security Incident Response Guide: <https://docs.aws.amazon.com/security-ir/latest/userguide/security-incident-response-guide.html>
- AWS Health user guide: <https://docs.aws.amazon.com/health/latest/ug/what-is-aws-health.html>
- Amazon Builders' Library: <https://aws.amazon.com/builders-library/>
