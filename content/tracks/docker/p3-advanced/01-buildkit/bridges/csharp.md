---
from: csharp
status: draft
concept: "Build caches, build secrets and target platforms"
from_way: "NuGet caches packages in ~/.nuget/packages, and private feeds use credentials in nuget.config."
native_way: "Cache ~/.nuget/packages with a cache mount and supply the feed credentials with a secret mount during restore."
sensei_tip: "dotnet publish -r linux-arm64 cross-compiles on an amd64 builder: combine FROM --platform=$BUILDPLATFORM with -a $TARGETARCH to avoid slow emulation."
---

| C# / .NET | BuildKit |
| --- | --- |
| `~/.nuget/packages` | `--mount=type=cache,target=/root/.nuget/packages` |
| `nuget.config` credentials | `--mount=type=secret` |
| `-r linux-arm64` | `ARG TARGETARCH` and `--platform` |
| `<Version>` in the csproj | `ARG VERSION` passed to `-p:Version` |
