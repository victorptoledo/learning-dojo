# Structured Logging with log/slog and Observability

A service in production must explain itself: what it is doing (logs), how much and how fast
(metrics), and where the time of one request went (traces). Since Go 1.21 the standard
library has **structured logging** in `log/slog`; the runtime exposes its own metrics; and
OpenTelemetry covers traces.

## Why structured logs

```text
2026/11/15 10:00:03 payment failed for order 42 after 3 retries: card declined
```

A human can read that, but a log system cannot filter "all failures of order 42" without a
regular expression. A **structured** log line is a message plus typed key-value attributes:

```json
{"time":"2026-11-15T10:00:03Z","level":"ERROR","msg":"payment failed","order":42,"retries":3,"err":"card declined"}
```

## log/slog

```go
logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
	Level: slog.LevelInfo,
}))
slog.SetDefault(logger)          // also routes the old log package through it

logger.Info("server started", "addr", ":8080", "version", version)
logger.Error("payment failed", "order", orderID, "retries", 3, "err", err)
logger.Debug("cache miss", "key", key)        // dropped at LevelInfo
```

- Four levels: `Debug`, `Info`, `Warn`, `Error`. Use a `slog.LevelVar` to change the level at
  run time without a restart.
- Two built-in handlers: `TextHandler` (`key=value`, nice in a terminal) and `JSONHandler`
  (for log systems). Choose by environment, write to standard output, and let the platform
  collect it.
- Since Go 1.26, `slog.NewMultiHandler` sends the same records to several handlers.

### Attributes

```go
logger.Info("request",
	slog.String("method", r.Method),
	slog.Int("status", status),
	slog.Duration("duration", elapsed),
	slog.Group("user", slog.String("id", user.ID), slog.String("plan", user.Plan)),
)
```

Typed attributes (`slog.String`, `slog.Int`, ...) avoid allocations and mistakes such as an
odd number of arguments (which `go vet` reports). `LogAttrs` is the fastest form for hot
paths.

### Loggers with context

`With` returns a logger that adds attributes to every record, the idiomatic way to carry
request data:

```go
reqLogger := logger.With("request_id", requestID, "route", r.Pattern)
reqLogger.Info("order created", "order", order.ID)
```

Pass loggers explicitly (a field of your server struct) or store a request-scoped one in the
context with a small helper. `InfoContext(ctx, ...)` gives handlers access to the context, so
a custom or OpenTelemetry handler can add the trace ID automatically.

### What to log, and what not

- One line per meaningful event: start-up and configuration (without secrets), each request
  (method, route, status, duration), every error that is handled, shutdown.
- An error is logged **once**, where it is handled; lower layers return it with context.
- **Never** log passwords, tokens, full card numbers or personal data. A type can implement
  `slog.LogValuer` to redact itself:

```go
type Token string

func (Token) LogValue() slog.Value { return slog.StringValue("REDACTED") }
```

## Metrics

Metrics answer "how many and how fast" cheaply: request counts by route and status, latency
histograms, queue depth, error rates. The common choices:

- **Prometheus** client (`prometheus/client_golang`): counters, gauges and histograms on a
  `/metrics` endpoint.
- **OpenTelemetry** metrics, exported to whatever backend you use.
- `expvar` in the standard library, for simple JSON counters at `/debug/vars`.

The runtime publishes its own measurements in **`runtime/metrics`**: heap size, GC cycles and
pause times, goroutine count (`/sched/goroutines:goroutines`, with more scheduler metrics
since Go 1.26), and more. Prometheus and OpenTelemetry clients export them for you.

## Traces

A trace follows one request across services; each step is a **span** with a start, a
duration and attributes. With OpenTelemetry you create spans around handlers and outgoing
calls (`otelhttp` wraps both), propagate the trace context in HTTP headers, and the context
carries the current span through your code, which is one more reason to pass `ctx`
everywhere.

## Health and debug endpoints

- `/healthz` (liveness: the process is alive) and `/readyz` (readiness: dependencies are
  reachable, the service can take traffic) for orchestrators.
- `net/http/pprof` exposes profiles (next lesson) and should listen on a **separate, internal
  port**, never on the public one.

## Summary

- Structured logs are a message plus key-value attributes; `log/slog` is the standard API,
  with Text and JSON handlers and levels.
- `logger.With` for request-scoped attributes, typed attributes on hot paths, `LogValuer` to
  redact, one log per handled error.
- Metrics for rates and latencies (Prometheus, OpenTelemetry, `runtime/metrics`), traces for
  one request across services, health endpoints for the orchestrator.

## Further reading

- Structured Logging with slog (Go blog): <https://go.dev/blog/slog>
- Package log/slog: <https://pkg.go.dev/log/slog>
- A Guide to Writing slog Handlers: <https://github.com/golang/example/blob/master/slog-handler-guide/README.md>
- Package runtime/metrics: <https://pkg.go.dev/runtime/metrics>
- OpenTelemetry for Go: <https://opentelemetry.io/docs/languages/go/>
- roadmap.sh, Go topic "slog": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/golang/content/slog@hFlbSSBMcJtjXNL8jAcbc.md>
