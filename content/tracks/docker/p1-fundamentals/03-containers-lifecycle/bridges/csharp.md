---
from: csharp
status: draft
concept: "Process lifecycle and graceful shutdown"
from_way: "IHostApplicationLifetime.ApplicationStopping fires when the generic host is asked to stop, from Ctrl+C or a Windows service stop."
native_way: "The .NET host maps SIGTERM to a graceful stop, so docker stop triggers ApplicationStopping and hosted services' StopAsync, as long as dotnet is PID 1."
sensei_tip: "The host's default shutdown timeout is 30 seconds but docker stop waits only 10: align HostOptions.ShutdownTimeout with docker stop -t (or stop_grace_period in Compose)."
---

| C# / .NET | Container |
| --- | --- |
| `ApplicationStopping` | the SIGTERM from `docker stop` |
| `HostOptions.ShutdownTimeout` | must fit in the stop grace period |
| Windows service recovery options | the restart policy |
| `Environment.Exit(1)` | exit code 1 |
