---
from: csharp
status: draft
concept: "Least privilege at run time"
from_way: "Code Access Security (gone in .NET Core) and the application pool identity limited what code could do on Windows servers."
native_way: "Linux containers rely on the kernel: cgroup limits, dropped capabilities, seccomp and AppArmor profiles, a non-root user and a read-only root file system."
sensei_tip: ".NET follows the cgroup memory limit for the GC; set DOTNET_GCHeapHardLimitPercent if the app shares the container with native memory-hungry libraries."
---

| C# / .NET | Runtime security |
| --- | --- |
| an application pool memory limit | `--memory` |
| `Environment.ProcessorCount` | follows `--cpus` |
| Windows ACLs on the app folder | `--read-only` and a volume for data |
| running as LocalSystem | `--user` non-root, `--cap-drop ALL` |
