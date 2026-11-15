---
from: csharp
status: draft
concept: "Solutions and project references"
from_way: "A .sln with several .csproj files, ProjectReference items, incremental builds in dependency order with msbuild."
native_way: "A solution tsconfig with references to composite projects, tsc -b builds them in order and incrementally."
sensei_tip: "TypeScript borrowed both words from C#: a solution tsconfig is your .sln, and references are ProjectReference items."
---

| C# | TypeScript |
| --- | --- |
| `Acme.sln` | Root `tsconfig.json` with `"files": []` and `references` |
| `<ProjectReference Include="..\Domain\Domain.csproj" />` | `"references": [{ "path": "../domain" }]` |
| `dotnet build Acme.sln` | `tsc -b` |
| `Directory.Build.props` | `tsconfig.base.json` with `extends` |
| `internal` + `InternalsVisibleTo` | Unlisted paths in `exports` |
