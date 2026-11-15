---
from: java
status: draft
concept: "A Pod is a co-located group, scaled as one"
from_way: "An application server hosts your WAR next to agents (a JMX exporter, an APM agent) on the same machine, and they share its network."
native_way: "A Pod groups the app container with helpers that must share its network and files; the whole group is scheduled, scaled and replaced together."
sensei_tip: "Handle SIGTERM in Spring Boot with server.shutdown=graceful, and keep the grace period longer than spring.lifecycle.timeout-per-shutdown-phase."
---

| Java world | Kubernetes |
| --- | --- |
| a `-javaagent` on the same JVM host | a sidecar container in the Pod |
| Flyway running before the app starts | an init container |
| a JVM shutdown hook | SIGTERM handling inside the grace period |
| Maven coordinates as identity | labels such as `app.kubernetes.io/name` |
