---
from: csharp
status: draft
concept: "Separate build and runtime environments"
from_way: "The SDK builds and publishes; servers get the ASP.NET Core runtime and the publish folder."
native_way: "FROM mcr.microsoft.com/dotnet/sdk AS build runs restore and publish; FROM mcr.microsoft.com/dotnet/aspnet copies the publish output."
sensei_tip: "Microsoft also publishes chiseled Ubuntu images (aspnet:10.0-noble-chiseled), the .NET equivalent of distroless: no shell, non-root by default."
---

| C# / .NET | Multi-stage |
| --- | --- |
| the .NET SDK | `FROM mcr.microsoft.com/dotnet/sdk AS build` |
| `dotnet publish -o /out` | in the build stage |
| the ASP.NET Core runtime | `FROM mcr.microsoft.com/dotnet/aspnet` |
| self-contained, trimmed publish | can run on `runtime-deps` or chiseled images |
