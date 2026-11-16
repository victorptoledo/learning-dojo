---
from: java
status: draft
concept: "Diagnose a service from the host"
from_way: "You use jps, jstack and VisualVM against a JVM that someone else installed and started."
native_way: "You find the JVM with ps or systemctl, read its logs with journalctl, check its sockets with ss and its memory against the cgroup limit."
sensei_tip: "In a container, a JVM without -XX:MaxRAMPercentage may size its heap for the host; check the cgroup limit before blaming a leak."
---

| Java world | Linux |
| --- | --- |
| `jps` | `ps -eo pid,cmd \| grep java` |
| `jstack PID` | `kill -QUIT PID` (thread dump to stdout) |
| `OutOfMemoryError` | the kernel OOM killer, exit 137 |
| a shutdown hook | handling SIGTERM |
