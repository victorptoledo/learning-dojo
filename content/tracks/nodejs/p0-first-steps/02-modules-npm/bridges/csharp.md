---
from: csharp
status: draft
concept: "Packages and project files"
from_way: "Namespaces and assemblies, PackageReference entries in the .csproj restored from NuGet, packages.lock.json optional, dotnet restore --locked-mode in CI."
native_way: "File modules with import/export, dependencies in package.json installed from npm into node_modules, package-lock.json always produced, npm ci in CI."
sensei_tip: "NuGet resolves the lowest version that satisfies a range; npm resolves the highest. Without a lockfile, npm installs drift much faster."
---

| C# / .NET | Node.js |
| --- | --- |
| `using Acme.Util;` | `import { x } from "./util.ts"` |
| `.csproj` `PackageReference` | `package.json` `dependencies` |
| `PrivateAssets="all"` (build-only) | `devDependencies` |
| NuGet | npm registry |
| `dotnet restore --locked-mode` | `npm ci` |
| `global.json` (SDK version) | `engines`, `.nvmrc` |
