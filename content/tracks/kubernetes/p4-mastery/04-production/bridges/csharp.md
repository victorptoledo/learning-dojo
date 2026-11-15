---
from: csharp
status: draft
concept: "Operating the platform, not only the app"
from_way: "Azure DevOps release pipelines push to App Service, and Azure handles the platform underneath."
native_way: "On AKS the control plane is managed, but node images, add-ons, upgrades, policies and GitOps (Flux is built in as an extension) are yours to run."
sensei_tip: "Use AKS auto-upgrade channels and planned maintenance windows, and Azure Policy for Kubernetes (Gatekeeper underneath) for house rules."
---

| Azure / .NET world | Kubernetes |
| --- | --- |
| release pipelines | GitOps with Flux or Argo CD |
| App Service platform updates | AKS upgrade channels |
| Azure Policy | Azure Policy for AKS / Gatekeeper / Kyverno |
| Application Insights | OpenTelemetry + Azure Monitor |
