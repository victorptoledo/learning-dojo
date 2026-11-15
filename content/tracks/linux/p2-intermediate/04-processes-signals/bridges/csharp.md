---
from: csharp
status: draft
concept: "Processes and stopping cleanly"
from_way: "Process.Start, Task and the thread pool, and the host's IHostApplicationLifetime for shutdown."
native_way: "fork and exec, signals, and exit codes 128 + N when a signal kills a process."
sensei_tip: "The .NET generic host already maps SIGTERM to a graceful shutdown (ApplicationStopping). Keep HostOptions.ShutdownTimeout shorter than systemd's or Kubernetes' grace period."
---

| C# | Linux |
| --- | --- |
| Process.Start("ls") | fork + exec |
| Environment.ProcessId | $$ |
| process.WaitForExit() | wait |
| PosixSignalRegistration.Create(SIGTERM, ...) | trap ... TERM |
| process.Kill() | kill -9 PID |
