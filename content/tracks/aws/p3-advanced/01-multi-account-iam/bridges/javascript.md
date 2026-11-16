---
from: javascript
status: draft
concept: "Accounts are the isolation boundary; guardrails set the maximum"
from_way: "One cloud project holds dev, staging and prod resources, told apart by prefixes."
native_way: "Accounts under Organizations, with OUs and SCPs; people and pipelines assume roles per account."
sensei_tip: "In the SDK v3 use `fromTemporaryCredentials({ params: { RoleArn } })` from `@aws-sdk/credential-providers` for cross-account calls; it refreshes on its own."
---

| Node.js world | AWS |
| --- | --- |
| env prefixes in one project | one account per environment |
| an org-wide ESLint config | SCPs as organization-wide rules |
| `fromTemporaryCredentials` | assuming a role in another account |
| GitHub teams | Identity Center groups |
