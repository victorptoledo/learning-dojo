---
from: csharp
status: draft
concept: "Kernel-level isolation"
from_way: "AppDomains (in .NET Framework) or separate processes on Windows isolate code; Hyper-V isolation runs Windows containers in a utility VM."
native_way: "Linux containers isolate processes with namespaces and limit them with cgroups, all on the shared host kernel; no VM is involved."
sensei_tip: ".NET reads cgroup limits for the GC heap and Environment.ProcessorCount. In tight containers, tune DOTNET_GCHeapHardLimit or use Server GC only when you have the cores."
---

| C# / .NET | Container |
| --- | --- |
| a Windows process | a Linux process in its own namespaces |
| Job Objects limit CPU and memory on Windows | cgroups limit CPU, memory and processes on Linux |
| `Environment.ProcessorCount` | follows the cgroup CPU limit |
| `OutOfMemoryException` | an OOM kill from the kernel when the cgroup limit is hit |
