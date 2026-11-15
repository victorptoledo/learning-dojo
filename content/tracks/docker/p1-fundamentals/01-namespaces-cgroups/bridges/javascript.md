---
from: javascript
status: draft
concept: "Kernel-level isolation"
from_way: "The browser sandbox or a V8 isolate hides one script from another inside the same process."
native_way: "Container isolation happens at the kernel level between whole processes: namespaces hide other processes, interfaces and files, and cgroups cap resources."
sensei_tip: "Recent Node.js versions read the cgroup memory limit when sizing the heap, but set --max-old-space-size explicitly in small containers to leave room for buffers and native memory."
---

| JavaScript | Container |
| --- | --- |
| V8 isolates in one process | separate processes isolated by the kernel |
| `os.cpus().length` | the host's cores; the cgroup CPU limit is in `/sys/fs/cgroup/cpu.max` |
| "JavaScript heap out of memory" | the heap limit; the cgroup limit kills the whole process (137) |
| `server.listen(3000)` | port 3000 of the container's own network namespace |
