---
from: java
status: draft
concept: "Accounts are the isolation boundary; guardrails set the maximum"
from_way: "One shared environment where every team's apps live side by side, separated by naming conventions."
native_way: "One account per environment and team under Organizations; SCPs cap what anyone can do; Identity Center gives people access."
sensei_tip: "Use `StsAssumeRoleCredentialsProvider` in the AWS SDK for Java to call another account: it refreshes the assumed role's credentials before they expire."
---

| Java world | AWS |
| --- | --- |
| one app server per environment | one account per environment |
| a corporate security policy document | SCPs and RCPs attached to OUs |
| LDAP groups | Identity Center groups and permission sets |
| `StsAssumeRoleCredentialsProvider` | cross-account roles |
