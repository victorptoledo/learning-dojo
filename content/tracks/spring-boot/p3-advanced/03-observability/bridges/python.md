---
from: python
status: draft
concept: "Seeing inside a running service"
from_way: "Logging, prometheus_client, OpenTelemetry instrumentation and a /health route."
native_way: "Actuator endpoints for health and metrics, Micrometer meters, and tracing with Micrometer Tracing or OpenTelemetry."
sensei_tip: "Actuator gives you /actuator/health and /actuator/prometheus without code; add your own Counter or Timer through MeterRegistry like prometheus_client metrics."
---

| Python | Spring Boot 4 |
| --- | --- |
| /health route | /actuator/health with readiness and liveness |
| prometheus_client.Counter | Counter.builder(...).register(registry) |
| opentelemetry-instrument | Micrometer Tracing / OpenTelemetry |
| structlog JSON | structured logging (logging.structured.format.console) |
