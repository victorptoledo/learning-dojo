---
from: csharp
status: reviewed
concept: "From Web Deploy to containers and slots"
from_way: "Publishing from Visual Studio to a server, with appsettings.Production.json holding connection strings."
native_way: "dotnet publish to a container image per commit, configuration from environment variables and Key Vault, EF Core migration bundles as a release step, and deployment slots or rolling updates."
sensei_tip: "dotnet ef migrations bundle produces a self-contained migration executable you can run once in the pipeline, before the new version starts."
---

| .NET habit | Deployment practice |
| --- | --- |
| `appsettings.Production.json` with secrets | environment variables, Key Vault |
| `Database.Migrate()` at start-up | an EF Core migration bundle step |
| publish from the IDE | build once in CI, promote |
| Azure Functions | watch cold starts and connection limits |
