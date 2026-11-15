---
from: java
status: draft
concept: "Least privilege at run time"
from_way: "-Xmx caps the heap, the Security Manager (now removed) once restricted what code could do, and the service account limits file access."
native_way: "cgroup limits cap the whole process, capabilities and seccomp restrict what the kernel will do for it, and a read-only root file system protects the code."
sensei_tip: "Set -XX:MaxRAMPercentage=75 so the heap follows --memory, leaving room for metaspace, thread stacks and direct buffers; a fixed -Xmx equal to the limit ends in OOM kills."
---

| Java | Runtime security |
| --- | --- |
| `-Xmx` | `--memory`, with the heap as a percentage of it |
| a thread pool size | `--pids-limit` stops runaway thread creation |
| Security Manager policies | capabilities, seccomp and AppArmor at the kernel level |
| writing to the JAR's folder | impossible with `--read-only`; use a volume |
