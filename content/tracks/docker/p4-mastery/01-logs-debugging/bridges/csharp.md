---
from: csharp
status: draft
concept: "Logs and diagnostics"
from_way: "Serilog to rolling files or the Windows Event Log, and Visual Studio attaching to a process on a server."
native_way: "Log with the console provider (JSON formatter) to stdout; use dotnet-counters, dotnet-trace and dotnet-dump from a debug container sharing the PID namespace."
sensei_tip: "The .NET diagnostic tools talk to the runtime over a socket in /tmp: share it with the tool container (a shared tmpfs or volume on /tmp) to run dotnet-dump against a distroless app."
---

| C# / .NET | Containers |
| --- | --- |
| Serilog file sink | the console sink with a JSON formatter |
| Event Viewer | `docker logs` and a central log system |
| `dotnet-dump collect` | from a debug container with `--pid container:api` |
| attaching Visual Studio | the container tools or a published debug port in development |
