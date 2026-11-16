---
from: java
status: draft
concept: "Emit metrics, structured logs and traces from the service"
from_way: "Log4j writes text logs to files, and JMX beans expose a few counters to whoever connects."
native_way: "Micrometer exposes Prometheus metrics, Logback writes JSON with the trace ID, and the OpenTelemetry Java agent traces every request without code changes."
sensei_tip: "Spring Boot's management.metrics.distribution.percentiles-histogram turns on histogram buckets; without them you cannot aggregate percentiles across Pods."
---

| Java world | Observability |
| --- | --- |
| JMX beans | Micrometer and /actuator/prometheus |
| Log4j text files | JSON logs to stdout with trace_id |
| MDC | trace context in every log line |
| `-javaagent` APM | the OpenTelemetry Java agent |
