---
from: java
status: draft
concept: "Graceful shutdown and health"
from_way: "Spring Boot's server.shutdown=graceful with spring.lifecycle.timeout-per-shutdown-phase, Actuator liveness and readiness groups, and a JVM that runs shutdown hooks on SIGTERM."
native_way: "No framework does it for you by default: listen to SIGTERM, flip readiness, server.close() plus closeIdleConnections, close resources, and force an exit before the grace period."
sensei_tip: "Write by hand what server.shutdown=graceful and Actuator's probes give Spring Boot; Fastify plugins such as close-with-grace help. The JVM also needs exec-form CMD to see SIGTERM, so that part is familiar."
---

| Spring Boot | Node.js |
| --- | --- |
| `server.shutdown=graceful` | `server.close()` + `closeIdleConnections()` |
| `timeout-per-shutdown-phase` | Forced exit timer |
| Actuator `/actuator/health/liveness` | `/live` |
| Actuator readiness state | `/ready` answering 503 while shutting down |
| `@PreDestroy` | Close resources in the shutdown function |
| Layered jar image | `npm ci --omit=dev` image |
