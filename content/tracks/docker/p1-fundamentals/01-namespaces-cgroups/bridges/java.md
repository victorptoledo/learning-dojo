---
from: java
status: draft
concept: "Kernel-level isolation"
from_way: "Thinking of Docker like the JVM: a virtual machine that emulates a platform and runs your code on top of it."
native_way: "A container is a normal Linux process isolated by namespaces (what it sees) and limited by cgroups (what it uses); there is no emulation layer, so code runs at native speed."
sensei_tip: "The JVM reads its cgroup limits: inside a 512 MB container, Runtime.maxMemory() reflects the container, not the host. Check it with java -XshowSettings:system -version."
---

| Java idea | Container reality |
| --- | --- |
| the JVM sandbox and class loaders | kernel namespaces: PID, network, mount, user ... |
| `-Xmx` caps the heap | a cgroup caps the whole process: heap, metaspace, threads, native memory |
| `OutOfMemoryError` | the kernel's OOM killer: exit code 137, no stack trace |
| one JVM per host | many isolated processes sharing one kernel |
