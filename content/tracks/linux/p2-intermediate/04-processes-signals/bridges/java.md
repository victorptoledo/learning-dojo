---
from: java
status: draft
concept: "Processes and stopping cleanly"
from_way: "ProcessBuilder to start programs, Thread and ExecutorService inside one JVM, and a shutdown hook that may or may not run."
native_way: "fork and exec, a PID per program, signals to talk to them, and trap in scripts; the JVM is one Linux process and receives the same signals."
sensei_tip: "The JVM runs its shutdown hooks on SIGTERM and SIGINT, but not on SIGKILL. Keep the work in a shutdown hook short: systemd and Kubernetes send SIGKILL when the grace period ends."
---

| Java | Linux |
| --- | --- |
| `new ProcessBuilder("ls").start()` | fork + exec |
| `ProcessHandle.current().pid()` | `$$` in bash, `/proc/self` |
| `process.waitFor()` | `wait` |
| `Runtime.getRuntime().addShutdownHook(...)` | `trap cleanup TERM INT EXIT` |
| `process.destroy()` / `destroyForcibly()` | `kill PID` (TERM) / `kill -9 PID` (KILL) |
| `jcmd PID Thread.print` | `kill -3 PID` prints the thread dump of a JVM |
