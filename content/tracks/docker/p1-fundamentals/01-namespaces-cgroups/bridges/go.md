---
from: go
status: draft
concept: "Kernel-level isolation"
from_way: "Goroutines share one process and one view of the machine; isolation is your own code's job."
native_way: "The whole Go process gets its own PID, network, mount and host-name view from namespaces, and cgroups cap its CPU and memory."
sensei_tip: "Since Go 1.25 the runtime sets GOMAXPROCS from the cgroup CPU limit. For memory, set GOMEMLIMIT a little below the container limit so the GC works harder before the OOM killer does."
---

| Go | Container |
| --- | --- |
| `runtime.NumCPU()` | host cores; GOMAXPROCS follows the cgroup CPU limit |
| `GOMEMLIMIT` | a soft target below the cgroup memory limit |
| `syscall.Unshare` | the system call runtimes use to create namespaces |
| one static binary | the process a container runs as PID 1 |
