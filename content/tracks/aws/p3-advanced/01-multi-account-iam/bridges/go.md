---
from: go
status: draft
concept: "Accounts are the isolation boundary; guardrails set the maximum"
from_way: "One account and one credentials file used by every tool."
native_way: "Many accounts; tools assume a role per account with temporary credentials."
sensei_tip: "Wrap `stscreds.NewAssumeRoleProvider` in `aws.NewCredentialsCache` so cross-account credentials are cached and refreshed instead of calling STS on every request."
---

| Go world | AWS |
| --- | --- |
| one `~/.aws/credentials` key | SSO profiles per account |
| `stscreds.NewAssumeRoleProvider` | cross-account roles |
| a policy enforced in code review | an SCP enforced by AWS |
| build tags per environment | accounts per environment |
