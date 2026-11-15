---
from: csharp
status: draft
concept: "Where your JVM really runs"
from_way: "mcr.microsoft.com/dotnet images, and the runtime as the platform below your code."
native_way: "The .NET runtime as a Linux process inside a cgroup and namespaces."
sensei_tip: ".NET reads cgroup limits: Environment.ProcessorCount and the GC heap limit follow the container. DOTNET_GCHeapHardLimitPercent fine-tunes the heap."
---

| C# | Linux |
| --- | --- |
| Environment.ProcessorCount | CPUs allowed by the cgroup |
| GC heap hard limit | a share of memory.max |
| Windows containers | Linux containers share the Linux kernel |
| Hyper-V isolation | VMs or Kata Containers |
