---
from: go
status: reviewed
concept: "slog, OpenTelemetry and Prometheus in Go"
from_way: "log.Printf lines and a manual timer printed at the end of a handler."
native_way: "slog's JSON handler with the trace id from the context, otelhttp wrapping handlers and clients, and client_golang histograms by route."
sensei_tip: "Wrap the http.Client's transport with otelhttp.NewTransport and the traceparent header is propagated on every outgoing call."
---

| Go | Observability |
| --- | --- |
| `log.Printf` | `slog.New(slog.NewJSONHandler(...))` |
| `context.Context` | carries the span and trace id |
| `otelhttp.NewHandler` | server spans and propagation |
| `promauto.NewHistogramVec` | RED metrics by route |
