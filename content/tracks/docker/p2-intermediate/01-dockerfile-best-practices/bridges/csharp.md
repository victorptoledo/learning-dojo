---
from: csharp
status: draft
concept: "Incremental builds and dependency caching"
from_way: "dotnet build restores packages into the NuGet cache and compiles incrementally."
native_way: "Copy the .sln and .csproj files first and run dotnet restore in its own layer; copy the sources and publish after, so code changes reuse the restore."
sensei_tip: "With several projects, copy every .csproj into its folder before restoring; a single COPY . . defeats the cache."
---

| C# / .NET | Dockerfile |
| --- | --- |
| `dotnet restore` | its own cached layer |
| `Directory.Packages.props` | copied with the project files |
| incremental build | the layer cache order |
| Roslyn analyzers | hadolint |
