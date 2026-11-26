---
from: java
status: draft
concept: "SLF4J, Logback, MDC and Micrometer"
from_way: "SLF4J loggers per class with Logback XML configuration, MDC for request ids, logstash encoders for JSON, Micrometer for metrics and Spring Boot Actuator endpoints."
native_way: "log/slog in the standard library, handlers chosen in code, logger.With instead of MDC, typed attributes for structured output, and Prometheus or OpenTelemetry clients for metrics."
sensei_tip: "MDC relies on thread-locals, which goroutines do not have. In Go the request logger travels explicitly, as a field or in the context."
---

| Java | Go |
| --- | --- |
| `LoggerFactory.getLogger(Foo.class)` | a `*slog.Logger` passed in, or `slog.Default()` |
| `log.info("order {} paid", id)` | `logger.Info("order paid", "order", id)` |
| `MDC.put("requestId", id)` | `logger.With("request_id", id)` |
| logback.xml + logstash encoder | `slog.NewJSONHandler(os.Stdout, opts)` |
| `@ToString.Exclude` / masking | `LogValue() slog.Value` returning a redacted value |
| Micrometer `Counter` | Prometheus `CounterVec` or OpenTelemetry meter |
| Actuator `/health` | your `/healthz` and `/readyz` handlers |
| JFR / JMX metrics | `runtime/metrics` |
