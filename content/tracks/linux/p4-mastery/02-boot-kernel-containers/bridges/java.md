---
from: java
status: draft
concept: "Where your JVM really runs"
from_way: "The JVM as the platform: write once, run anywhere, with the operating system and the container as details below it."
native_way: "The JVM is a Linux process: the kernel schedules its threads, a cgroup limits its memory and CPU, and namespaces decide what it can see."
sensei_tip: "Since Java 10 the JVM reads cgroup limits, so -XX:MaxRAMPercentage=75 sizes the heap from the container's memory, not the host's. Leave room for metaspace, threads and direct buffers, or the cgroup OOM killer ends the JVM with 137."
---

| Java view | Linux reality |
| --- | --- |
| `Runtime.getRuntime().availableProcessors()` | CPUs allowed by the cgroup (`cpu.max`) |
| `-Xmx` / `MaxRAMPercentage` | inside the cgroup's `memory.max` |
| `OutOfMemoryError` | the JVM's own heap limit |
| exit code 137, no Java error | the kernel's OOM killer (cgroup limit) |
| `System.getenv("HOSTNAME")` | the uts namespace's hostname |
| a container image | layers stacked by overlayfs in a mount namespace |
