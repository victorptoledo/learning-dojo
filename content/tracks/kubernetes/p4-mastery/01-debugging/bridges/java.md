---
from: java
status: draft
concept: "Debugging a process you cannot log in to"
from_way: "SSH to the server, tail catalina.out, run jstack and attach a remote debugger."
native_way: "kubectl describe and events show what the platform saw, kubectl logs (with --previous) replaces tail, and kubectl debug adds a container with your tools next to the JVM."
sensei_tip: "Use an ephemeral container from a JDK image with --target to run jcmd <pid> Thread.print or GC.heap_info against the running JVM without restarting it."
---

| Java world | Kubernetes |
| --- | --- |
| `tail -f catalina.out` | `kubectl logs -f` |
| reading hs_err_pid after a crash | `kubectl logs --previous`, exit code in describe |
| `jstack` over SSH | `kubectl debug --target` with a JDK image, then `jcmd` |
| remote debugging on port 5005 | `kubectl port-forward pod/x 5005` |
