---
from: javascript
status: draft
concept: "pino or winston logging"
from_way: "pino or winston loggers writing JSON, child loggers with bindings per request, redact options for secrets, prom-client for metrics and OpenTelemetry for Node."
native_way: "log/slog in the standard library writes JSON or text, With creates child loggers, LogValuer redacts, and the same Prometheus and OpenTelemetry ideas apply."
sensei_tip: "pino.child({ reqId }) and logger.With(\"req_id\", id) are the same idea; slog just ships with Go, so there is no logging library to choose."
---

| Node | Go |
| --- | --- |
| `const log = pino()` | `slog.New(slog.NewJSONHandler(os.Stdout, nil))` |
| `log.info({ order: 42 }, "paid")` | `logger.Info("paid", "order", 42)` |
| `log.child({ reqId })` | `logger.With("req_id", reqID)` |
| `pino({ redact: ["password"] })` | `LogValue()` on sensitive types |
| `log.level = "debug"` | `levelVar.Set(slog.LevelDebug)` |
| `prom-client` | `prometheus/client_golang` |
| `@opentelemetry/sdk-node` | `go.opentelemetry.io/otel` |
