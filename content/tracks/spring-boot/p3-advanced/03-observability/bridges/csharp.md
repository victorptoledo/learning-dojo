---
from: csharp
status: draft
concept: "Seeing inside a running service"
from_way: "Health checks middleware, System.Diagnostics.Metrics and OpenTelemetry .NET."
native_way: "Actuator health groups, Micrometer and Micrometer Tracing."
sensei_tip: "MapHealthChecks with tags for ready and live is Actuator's health groups. Meter and Counter<T> map to Micrometer's MeterRegistry."
---

| C# | Spring Boot 4 |
| --- | --- |
| AddHealthChecks() | Actuator + HealthIndicator |
| Meter / Counter<T> | MeterRegistry / Counter |
| AddOpenTelemetry() | Micrometer Tracing + OTLP exporter |
| /healthz/ready | /actuator/health/readiness |
