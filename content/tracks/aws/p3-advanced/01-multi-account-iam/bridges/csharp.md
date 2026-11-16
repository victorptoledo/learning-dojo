---
from: csharp
status: draft
concept: "Accounts are the isolation boundary; guardrails set the maximum"
from_way: "Azure subscriptions under management groups, with Azure Policy and RBAC role assignments."
native_way: "AWS accounts under OUs, with SCPs and RCPs, IAM Identity Center permission sets and permissions boundaries."
sensei_tip: "The mapping is close: management groups ≈ OUs, subscriptions ≈ accounts, Azure Policy deny ≈ SCPs; but AWS SCPs never grant, they only cap."
---

| .NET world | AWS |
| --- | --- |
| management groups | organizational units |
| subscriptions | accounts |
| Azure Policy (deny) | SCPs and RCPs |
| Entra ID PIM | Identity Center permission sets with short sessions |
