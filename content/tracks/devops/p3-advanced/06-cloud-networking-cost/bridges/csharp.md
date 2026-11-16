---
from: csharp
status: draft
concept: "Networks, load balancers and bills are part of the design"
from_way: "Azure VNets, NSGs and Application Gateways set up in the portal by the infrastructure team."
native_way: "The same pieces declared in Terraform or Bicep, with tags on every resource and a budget alert per team."
sensei_tip: "Azure Cost Management's views by tag only work if tags are enforced: add an Azure Policy that denies untagged resources."
---

| C# world | Cloud |
| --- | --- |
| a VNet in the portal | a VPC in code |
| NSGs | security groups |
| Application Gateway | a layer 7 load balancer |
| Cost Management | FinOps dashboards and budgets |
