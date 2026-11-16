---
from: csharp
status: draft
concept: "Infrastructure has a build file and a dependency graph"
from_way: "ARM templates or Bicep files deploy Azure resources, or you click in the portal."
native_way: "Terraform works the same way across every cloud and SaaS API, with an explicit state and a plan before each change."
sensei_tip: "Bicep has what-if, Terraform has plan: never apply to production without reading one."
---

| C# world | Terraform |
| --- | --- |
| an ARM template or Bicep | `.tf` files |
| a Bicep module | a Terraform module |
| what-if | `terraform plan` |
| a NuGet package version | a provider version constraint |
