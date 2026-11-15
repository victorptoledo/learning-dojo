---
from: csharp
status: draft
concept: "Packaging an application with its runtime"
from_way: "dotnet publish produces a framework-dependent or self-contained app; the server still needs the right .NET runtime or the right OS for self-contained builds."
native_way: "An image based on mcr.microsoft.com/dotnet/aspnet carries the runtime and your published output, and runs on any machine with a container engine."
sensei_tip: "The .NET SDK can build images without a Dockerfile (dotnet publish with the container target), but learn the Dockerfile first: it is what every other stack uses."
---

| C# / .NET | Docker |
| --- | --- |
| install the .NET runtime on the server | `FROM mcr.microsoft.com/dotnet/aspnet` |
| `dotnet publish -c Release` | a build stage `FROM mcr.microsoft.com/dotnet/sdk` |
| NuGet | container registries for images |
| IIS or a Windows service | a container started by the engine or an orchestrator |
