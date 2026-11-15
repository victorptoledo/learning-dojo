---
from: csharp
status: draft
concept: "Layered, content-addressed artifacts"
from_way: "NuGet packages are versioned and cached, but dotnet publish output is one folder that you copy as a whole."
native_way: "Copy the project file and restore first, then copy the code and publish: the restore layer is cached until dependencies change."
sensei_tip: "Microsoft publishes .NET images with stable tags (9.0) and patch tags (9.0.x); pin a digest in production pipelines so a patch update is a reviewed change."
---

| C# / .NET | Image |
| --- | --- |
| NuGet package and version | repository and tag |
| `packages.lock.json` hashes | layer and manifest digests |
| `dotnet restore` output | a cached layer |
| the NuGet global packages folder | the engine's layer store |
