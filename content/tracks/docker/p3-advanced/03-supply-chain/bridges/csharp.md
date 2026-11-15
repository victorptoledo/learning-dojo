---
from: csharp
status: draft
concept: "Trusting third-party components"
from_way: "dotnet list package --vulnerable checks NuGet references, and signed packages come from verified publishers."
native_way: "Image scanners check the NuGet assemblies in the published output and the OS packages of the base image; signatures and digests identify trusted images."
sensei_tip: "Microsoft rebuilds its .NET images for every OS patch; pin a digest and let Renovate or Dependabot propose the new one, so patches arrive as reviewed changes."
---

| C# / .NET | Image supply chain |
| --- | --- |
| `dotnet list package --vulnerable` | `trivy image`, `docker scout` |
| NuGet package signing | cosign or notation for images |
| a private NuGet feed | a private registry |
| `PackageReference` versions | tags and digests |
