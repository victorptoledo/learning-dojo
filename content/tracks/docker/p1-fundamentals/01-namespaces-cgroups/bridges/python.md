---
from: python
status: draft
concept: "Kernel-level isolation"
from_way: "A virtualenv only changes which packages Python imports; every process still sees the whole machine."
native_way: "Namespaces give the process its own process list, network stack, host name and root file system; cgroups cap its CPU and memory."
sensei_tip: "os.cpu_count() and os.process_cpu_count() both ignore the cgroup CPU quota, so a 1-CPU container on a 16-core host still reports 16. Read /sys/fs/cgroup/cpu.max before sizing a worker pool."
---

| Python | Container |
| --- | --- |
| `venv` isolates packages | the mount namespace isolates the whole file system |
| `multiprocessing` workers | processes in the container's PID namespace and cgroup |
| `MemoryError` | usually an OOM kill (exit 137) before Python can raise anything |
| `socket.bind(("0.0.0.0", 8000))` | binds in the container's own network namespace |
