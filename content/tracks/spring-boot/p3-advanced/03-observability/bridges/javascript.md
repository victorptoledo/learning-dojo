---
from: javascript
status: draft
concept: "Seeing inside a running service"
from_way: "prom-client, terminus health checks and OpenTelemetry for Node."
native_way: "Actuator, Micrometer and tracing built into Spring Boot."
sensei_tip: "@nestjs/terminus health indicators are Actuator HealthIndicators; prom-client histograms are Micrometer Timers."
---

| JavaScript / TypeScript | Spring Boot 4 |
| --- | --- |
| terminus health checks | HealthIndicator beans |
| prom-client Histogram | Timer |
| @opentelemetry/sdk-node | Micrometer Tracing |
| pino with trace ids | logs with traceId and spanId |
