---
from: csharp
status: draft
concept: "Build, publish and analyzers"
from_way: "dotnet build and dotnet publish compile to IL, with ReadyToRun or Native AOT; type checking is part of every build."
native_way: "Run the sources on Node, compile with tsc or bundle; type-checking is a separate tsc step, which TypeScript 7 makes fast with a native, parallel compiler."
sensei_tip: "TypeScript 7 is TypeScript's Roslyn moment in reverse: the compiler left its own language for Go to become about ten times faster."
---

| C# | TypeScript |
| --- | --- |
| `dotnet publish -c Release` | `tsc -p tsconfig.build.json` or a bundler |
| `<TreatWarningsAsErrors>` | `tsc` and `eslint` failing the CI job |
| `global.json` SDK pinning | Exact `typescript` version in `devDependencies` |
| Portable PDBs | Source maps |
