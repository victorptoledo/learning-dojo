---
from: java
status: draft
concept: "Seeing inside a running service"
from_way: "Grepping log files on the server, a hand-written /status servlet, JMX consoles, and Sleuth for trace ids."
native_way: "Actuator health and probes, Micrometer metrics scraped by Prometheus, the Observation API, OpenTelemetry tracing over OTLP, and structured logs with trace ids."
sensei_tip: "Add one business metric per important flow (orders placed, payments failed): it is what you will look at during an incident."
---

| Before | Spring Boot 4 |
| --- | --- |
| `/status` servlet | `/actuator/health` (+ liveness and readiness) |
| Counters in static fields | Micrometer `Counter`, `Timer`, `Gauge` |
| Spring Cloud Sleuth | Micrometer Tracing / OpenTelemetry |
| Plain text logs | `logging.structured.format.console=ecs` |
