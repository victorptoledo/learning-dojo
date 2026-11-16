---
from: java
status: reviewed
concept: "Micrometer, tracing and structured logs in Spring Boot"
from_way: "Plain-text logs from Logback, a stack trace to read, and no idea which browser request caused it."
native_way: "Actuator with Micrometer metrics, Micrometer Tracing (or the OpenTelemetry agent) propagating traceparent, structured JSON logging with the trace id in the MDC."
sensei_tip: "Spring Boot 3.4+ has logging.structured.format.console=ecs (or logstash); with tracing on, traceId and spanId appear in every line."
---

| Spring Boot | Observability |
| --- | --- |
| `http.server.requests` metric | RED metrics with the route template as `uri` |
| Micrometer Tracing | W3C `traceparent` in and out |
| MDC `traceId` | the id in every log line |
| `/actuator/prometheus` | the scrape endpoint |
