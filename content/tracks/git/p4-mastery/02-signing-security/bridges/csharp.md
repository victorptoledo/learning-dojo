---
from: csharp
status: draft
concept: "Signing and secrets"
from_way: "Connection strings in appsettings.json or web.config, committed with the project."
native_way: "dotnet user-secrets for development, environment variables or Azure Key Vault in production, and appsettings.json without secrets."
sensei_tip: "dotnet user-secrets stores development secrets in your profile folder, outside the repository, and ASP.NET Core loads them automatically in Development."
---

| .NET habit | Git practice |
| --- | --- |
| connection string in `appsettings.json` | `dotnet user-secrets` / Key Vault |
| Authenticode-signed assemblies | signed commits and tags for the source |
| `.pfx` certificate in the repository | never; rotate it if it was committed |
| Azure DevOps credential scanner | host-side secret scanning |
