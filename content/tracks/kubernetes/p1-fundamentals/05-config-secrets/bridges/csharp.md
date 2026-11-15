---
from: csharp
status: draft
concept: "Configuration outside the artifact"
from_way: "appsettings.{Environment}.json, user secrets in development and Azure Key Vault in production."
native_way: "ConfigMaps and Secrets feed the .NET configuration system as environment variables or files; Key Vault can sync into Secrets."
sensei_tip: "Use double underscores for nesting in env vars (ConnectionStrings__Orders) and AddKeyPerFile to read a mounted Secret folder; reloadOnChange picks up rotated files."
---

| .NET world | Kubernetes |
| --- | --- |
| `appsettings.Production.json` | a ConfigMap mounted as a file |
| `ConnectionStrings__Orders` env | `secretKeyRef` |
| Azure Key Vault provider | Secrets Store CSI driver or External Secrets |
| `IOptionsMonitor` reload | a mounted file that updates in place |
