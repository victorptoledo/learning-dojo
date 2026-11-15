---
from: csharp
status: draft
concept: "Debugging a process you cannot log in to"
from_way: "RDP to the server, open Event Viewer and attach Visual Studio's remote debugger."
native_way: "Events and describe replace Event Viewer, kubectl logs replaces the log files, and kubectl debug brings diagnostic tools into the Pod."
sensei_tip: "dotnet-trace, dotnet-counters and dotnet-dump work from an ephemeral container with --target; they talk to the runtime through its diagnostic socket."
---

| .NET world | Kubernetes |
| --- | --- |
| Event Viewer | `kubectl get events`, `describe` |
| log files on the server | `kubectl logs` |
| a memory dump with ProcDump | `dotnet-dump` from an ephemeral container |
| Visual Studio remote debugger | `kubectl port-forward` + the debugger |
