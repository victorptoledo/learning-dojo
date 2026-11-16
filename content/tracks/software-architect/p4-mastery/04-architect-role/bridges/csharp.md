---
from: csharp
status: draft
concept: "The architect's role and governance"
from_way: "An architecture review board that approves every new project and every NuGet package."
native_way: "`dotnet new` templates and shared `Directory.Build.props` as the paved road, NetArchTest rules in a shared package, central package management, ADRs and an advice forum."
sensei_tip: "`Directory.Packages.props` (central package management) plus a shared template gives you version convergence and defaults without a board: deviations are visible in one file per repository."
---

| .NET world | Governance idea |
| --- | --- |
| `dotnet new` templates | paved road |
| `Directory.Build.props`, analyzers | defaults and checks in every build |
| NetArchTest rules package | fitness functions as governance |
| central package management | technology radar in code |
| `docs/adr` and an advisory forum | the advice process |
| spikes and prototypes | staying technical |
