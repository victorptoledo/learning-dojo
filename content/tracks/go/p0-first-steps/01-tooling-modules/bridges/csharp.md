---
from: csharp
status: draft
concept: "Projects, packages and the SDK"
from_way: "A .csproj and a .sln, NuGet packages, dotnet build, test and format, and a .NET runtime (or a self-contained publish) on the server."
native_way: "go.mod per module, modules fetched by path through a proxy, the go command for everything, and a static binary by default."
sensei_tip: "A Go module is closer to a NuGet package than to a .csproj: one go.mod usually holds many packages (folders)."
---

| C# / .NET | Go |
| --- | --- |
| `.csproj` | `go.mod` |
| namespace | package (one folder) |
| NuGet | modules by import path, via `proxy.golang.org` |
| `dotnet add package X` | `go get module@version` |
| `dotnet build` / `dotnet test` | `go build ./...` / `go test ./...` |
| `public` / `internal` | Upper case names / `internal/` folders |
| `dotnet format` | `gofmt` |
| `dotnet publish --self-contained` | `go build` (always self-contained) |
