---
from: go
status: draft
concept: "Emit metrics, structured logs and traces from the service"
from_way: "log.Printf lines and expvar counters."
native_way: "client_golang exposes Prometheus metrics, log/slog writes JSON with the trace ID, and otelhttp wraps handlers and clients to propagate trace context."
sensei_tip: "Pass context.Context everywhere: in Go, the trace context travels in it, and a call without ctx breaks the trace."
---

| Go world | Observability |
| --- | --- |
| `log.Printf` | `slog` with a JSON handler |
| `expvar` | `client_golang` metrics |
| `context.Context` | trace context propagation |
| `httptrace` | otelhttp spans |
