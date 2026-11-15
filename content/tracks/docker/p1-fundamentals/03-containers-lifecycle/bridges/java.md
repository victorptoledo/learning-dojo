---
from: java
status: draft
concept: "Process lifecycle and graceful shutdown"
from_way: "Runtime.getRuntime().addShutdownHook runs on SIGTERM or System.exit, and Spring Boot's graceful shutdown finishes in-flight requests."
native_way: "docker stop sends SIGTERM to PID 1 and waits 10 seconds; the JVM runs its shutdown hooks only if it is PID 1 or receives the signal from an init."
sensei_tip: "Start the JVM with the exec form (ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]) and set server.shutdown=graceful with a timeout below the container's stop timeout."
---

| Java | Container |
| --- | --- |
| shutdown hooks | run on the SIGTERM from `docker stop` |
| `kill -9` | `SIGKILL` after the grace period: no hooks |
| `System.exit(1)` | exit code 1, which triggers `on-failure` restarts |
| a service wrapper restarting the JVM | the restart policy |
