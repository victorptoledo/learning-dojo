---
from: csharp
status: draft
concept: "Namespaces, assemblies and NuGet"
from_way: "Namespaces independent of folders, using directives, assemblies (DLLs) as the unit of deployment, internal visibility, dotnet pack and dotnet nuget push, and dotnet tool install for CLIs."
native_way: "Packages tied to folders, import statements that run code, wheels as the unit of distribution, underscore names for internals, uv build and uv publish, and uv tool install for CLIs."
sensei_tip: "dotnet tool install and uv tool install solve the same problem: a command-line tool with its own isolated dependencies, on the PATH."
---

| C# | Python |
| --- | --- |
| `namespace Acme.Invoicer` | the package folder `invoicer/` |
| `using Acme.Invoicer;` | `import invoicer` / `from invoicer import ...` |
| `internal` | a leading underscore (convention) |
| assembly / `.nupkg` | wheel (`.whl`) |
| `dotnet pack` | `uv build` |
| `dotnet nuget push` | `uv publish` |
| `<PackAsTool>true</PackAsTool>` | `[project.scripts]` |
| `dotnet tool install -g invoicer` | `uv tool install invoicer` |
