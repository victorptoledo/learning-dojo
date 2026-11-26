---
from: csharp
status: draft
concept: "Publishing .NET services"
from_way: "dotnet publish with trimming or Native AOT, chiseled Ubuntu images, appsettings.json with environment overrides and IOptions validation, dotnet list package --vulnerable, and Roslyn analyzers."
native_way: "go build always produces a native, self-contained binary; configuration is read into a validated struct; govulncheck checks reachable vulnerabilities; go vet and staticcheck play the analyzers' role."
sensei_tip: "Native AOT is an opt-in with trade-offs in .NET; in Go it is the only mode, and reflection-heavy libraries are rare because of it."
---

| .NET | Go |
| --- | --- |
| `dotnet publish -p:PublishAot=true` | `CGO_ENABLED=0 go build` |
| chiseled `runtime-deps` image | distroless static image |
| `appsettings.json` + `IOptions<T>` with validation | `LoadConfig()` returning `(Config, error)` |
| `dotnet list package --vulnerable` | `govulncheck` |
| Roslyn analyzers | `go vet`, staticcheck |
| `IHostedService` stop on SIGTERM | `signal.NotifyContext` + `Shutdown` |
| `.NET Upgrade Assistant` | `go fix` |
