---
from: go
status: draft
concept: "Seeing inside a running service"
from_way: "expvar, the Prometheus client library and OpenTelemetry Go."
native_way: "Actuator, Micrometer and tracing auto-configured."
sensei_tip: "Where a Go service registers every metric and handler by hand, Spring Boot auto-instruments HTTP, JDBC and the JVM; you add only business metrics."
---

| Go | Spring Boot 4 |
| --- | --- |
| promhttp.Handler() | /actuator/prometheus |
| prometheus.NewCounter | Counter.builder(...) |
| otelhttp middleware | automatic observation of requests |
| /healthz handler | /actuator/health |
