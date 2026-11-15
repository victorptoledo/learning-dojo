---
from: java
status: draft
concept: "Logs and diagnostics"
from_way: "Logback writes rolling files under logs/, and you attach jstack, jcmd or a remote debugger to a JVM on a server you can SSH into."
native_way: "Log to the console appender (JSON encoder) and let the platform collect it; diagnose with docker logs, inspect and events, and run jcmd through docker exec or a debug container."
sensei_tip: "Add -XX:+ExitOnOutOfMemoryError so a JVM that runs out of heap exits and is restarted, instead of limping along; the exit code and logs then tell the story."
---

| Java | Containers |
| --- | --- |
| a rolling file appender | the console appender to stdout |
| `jstack PID` over SSH | `docker exec api jcmd 1 Thread.print` |
| a remote debugger on port 5005 | publish the port on 127.0.0.1 in development only |
| heap dumps in a local folder | a volume for `-XX:HeapDumpPath` |
