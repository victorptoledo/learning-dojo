---
from: java
status: draft
concept: "Health groups and sized containers"
from_way: "One /health endpoint for the load balancer, and -Xmx set by hand on each server."
native_way: "Separate liveness and readiness endpoints feed the probes, and the JVM sizes its heap from the container's memory limit."
sensei_tip: "Spring Boot exposes /actuator/health/liveness and /actuator/health/readiness on Kubernetes; use -XX:MaxRAMPercentage=75 instead of -Xmx so the heap follows the limit and leaves room for metaspace and threads."
---

| Java world | Kubernetes |
| --- | --- |
| `/actuator/health` | readiness and liveness health groups |
| `-Xmx1g` | `MaxRAMPercentage` with a memory limit |
| `java.lang.OutOfMemoryError` | an app error; an OOMKilled exit 137 is the container limit |
| slow context start-up | a startup probe |
