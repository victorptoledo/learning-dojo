---
from: csharp
status: draft
concept: "Versions and release tags"
from_way: "AssemblyVersion and the NuGet package version edited in the .csproj before every release."
native_way: "Compute them from tags with MinVer or Nerdbank.GitVersioning: tag v1.5.0 and the build is 1.5.0."
sensei_tip: "MinVer reads git describe-style information at build time. CI needs the tags, so check out with full history (fetch-depth: 0 in GitHub Actions), or every build is 0.0.0."
---

| .NET habit | Git |
| --- | --- |
| `<Version>1.5.0</Version>` by hand | MinVer from the tag `v1.5.0` |
| pre-release `1.5.0-beta.1` | tag `v1.5.0-beta.1` |
| NuGet versions are immutable | so are pushed tags |
| Azure DevOps build number | `git describe --tags` |
