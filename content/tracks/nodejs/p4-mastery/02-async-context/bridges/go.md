---
from: go
status: draft
concept: "Implicit context instead of ctx parameters"
from_way: "context.Context is passed explicitly as the first argument, with values for request ids, slog for structured logs, and OpenTelemetry spans stored in the context."
native_way: "AsyncLocalStorage carries the context implicitly through the async flow; pino adds it to logs; OpenTelemetry stores spans in it."
sensei_tip: "Go made context explicit on purpose; Node.js makes it implicit. Explicit is easier to follow, implicit needs no cooperation from intermediate libraries. Keep the store small and read it only at the edges (logging, tracing)."
---

| Go | Node.js |
| --- | --- |
| `context.WithValue(ctx, key, id)` | `als.run({ requestId: id }, fn)` |
| `ctx.Value(key)` | `als.getStore()` |
| `slog.With("requestId", id)` | `logger.child({ requestId })` |
| `log/slog` JSON handler | pino |
| `otel.Tracer(...).Start(ctx, ...)` | OpenTelemetry Node.js SDK |
