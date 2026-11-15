---
from: csharp
status: draft
concept: "One definition, many environments"
from_way: "web.config transforms and appsettings.{Environment}.json layered on a base file."
native_way: "Kustomize patches are config transforms for Kubernetes YAML; Helm values files are per-environment settings for a templated package."
sensei_tip: "Helm charts are the NuGet packages of Kubernetes: install third-party software from charts, and keep your own services in Kustomize if your team prefers plain YAML."
---

| .NET world | Kubernetes |
| --- | --- |
| `web.Release.config` transforms | Kustomize patches |
| `appsettings.Production.json` | an overlay or a values file |
| NuGet packages | Helm charts |
| a NuGet feed | an OCI chart registry |
