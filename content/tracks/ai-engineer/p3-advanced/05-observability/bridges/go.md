---
from: go
status: draft
concept: "See what the model saw"
from_way: "slog, Prometheus metrics and OpenTelemetry traces in services."
native_way: "Spans around every model, retrieval and tool call with gen_ai attributes; histograms for latency and counters for tokens and cost."
sensei_tip: "Wrap your model client interface in a tracing decorator that starts a span, records token usage from the response and ends it; every caller gets observability for free."
---

| Go | LLM observability |
| --- | --- |
| `slog` with attributes | structured logs |
| `otel.Tracer("chat").Start(ctx, ...)` | a model-call span |
| `prometheus.Histogram` | latency |
| `prometheus.Counter` | tokens and cost |
| a decorator over the client interface | instrumentation in one place |
