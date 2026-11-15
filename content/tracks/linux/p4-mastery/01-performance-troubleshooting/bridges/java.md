---
from: java
status: draft
concept: "Finding why a service is slow"
from_way: "JVM tools: VisualVM or JDK Mission Control, GC logs, thread dumps with jstack, and heap dumps."
native_way: "Look at the machine first with the USE method and the standard tools, then zoom into the JVM process with the Java tools when the machine points at it."
sensei_tip: "If top shows the java process at 400% CPU, run top -H -p PID to find the hot thread IDs, convert one to hex (printf '%x' TID) and look for nid=0x... in a jcmd PID Thread.print dump."
---

| Java tool | Linux equivalent or partner |
| --- | --- |
| VisualVM CPU view | `top`, `pidstat`, `perf top` |
| GC logs and heap usage | `free -h`, `vmstat` (swap), OOM killer messages |
| `jstack` / `jcmd Thread.print` | `top -H`, `strace -f -p` |
| `java.io.IOException: Too many open files` | `ulimit -n`, `/proc/PID/limits`, `LimitNOFILE=` |
| JFR file I/O events | `iostat -xz`, `lsof -p` |
| `-XX:MaxRAMPercentage` | the cgroup memory limit it reads |
