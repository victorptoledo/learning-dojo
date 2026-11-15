---
from: csharp
status: draft
concept: "Declared state instead of start scripts"
from_way: "IIS or a Windows service hosts the app, and an Azure App Service slot swap ships the new version."
native_way: "A Deployment declares the image and replicas; Kubernetes keeps them running across nodes and replaces them gradually on a new version."
sensei_tip: "ASP.NET Core in a Linux container behind a Service is the usual shape on AKS; keep configuration in environment variables, which the configuration system reads by default."
---

| .NET world | Kubernetes |
| --- | --- |
| an IIS site or a Windows service | a container in a Pod |
| an App Service instance count | `replicas` of a Deployment |
| a deployment slot swap | a rolling update (or a blue-green switch of a Service) |
| `appsettings.Production.json` | a ConfigMap mounted as a file or env vars |
