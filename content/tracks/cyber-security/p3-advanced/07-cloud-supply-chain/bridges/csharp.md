---
from: csharp
status: draft
concept: "Trust what you deploy and what deploys it"
from_way: "NuGet packages with floating versions from several feeds, and a service principal secret stored in the pipeline."
native_way: "Central package management with lock files, package source mapping, dotnet list package --vulnerable in CI, and workload identity federation."
sensei_tip: "NuGet **package source mapping** tells the client which feed each package prefix may come from, which closes dependency confusion between a private and the public feed."
---

| C# | Supply chain practice |
| --- | --- |
| floating versions 1.* | packages.lock.json with locked mode |
| several feeds without mapping | package source mapping |
| no vulnerability check | dotnet list package --vulnerable |
| client secret for Azure in CI | workload identity federation |
| unsigned packages | signed packages and trusted signers |
