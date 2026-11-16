---
from: java
status: draft
concept: "Processes and threads in Java"
from_way: "Creating a platform thread per task."
native_way: "A platform Thread is an OS thread with its own stack (about 1 MiB by default); virtual threads (Java 21+) are scheduled by the JVM on a few carrier threads; ProcessBuilder starts OS processes."
sensei_tip: "Use virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`) for blocking I/O work and a bounded pool sized to cores for CPU work."
---

| In Java | Underneath |
| --- | --- |
| `new Thread(r).start()` | an OS thread |
| `Thread.ofVirtual().start(r)` | a JVM-scheduled virtual thread |
| `ProcessBuilder` | fork/exec (posix_spawn) of a child process |
| `Thread.setPriority` | a hint the OS may ignore |
| `Runtime.availableProcessors()` | cores visible, including cgroup limits |
