---
from: csharp
status: draft
concept: "Describing a multi-service environment"
from_way: "Visual Studio's container tools or .NET Aspire orchestrate several projects; user secrets keep passwords out of appsettings.json."
native_way: "compose.yaml is the portable description: services, networks, volumes and secrets that any machine with Docker can run, IDE or not."
sensei_tip: "ASP.NET Core configuration reads environment variables with double underscores for sections: ConnectionStrings__Default maps to ConnectionStrings:Default."
---

| C# / .NET | Compose |
| --- | --- |
| `appsettings.Development.json` | `environment:` and override files |
| user secrets (`dotnet user-secrets`) | Compose secrets mounted as files |
| launch profiles | Compose profiles |
| `ConnectionStrings:Default` | `ConnectionStrings__Default: Host=db;...` |
