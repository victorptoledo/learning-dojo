---
from: go
status: draft
concept: "Logging before log/slog"
from_way: "log.Printf with formatted strings, a third-party structured logger (zap, zerolog, logrus) chosen per team, adapters between them, and runtime.ReadMemStats polled by hand for metrics."
native_way: "log/slog (1.21) as the common API, slog.SetDefault to route the log package through it, NewMultiHandler (1.26) to fan out, zap or zerolog only as handlers when speed demands it, and runtime/metrics."
sensei_tip: "Libraries should accept a *slog.Logger (or use slog.Default) instead of importing zap or logrus: then the application chooses the output, and nobody needs adapters."
---

| Older Go | Go 1.27 |
| --- | --- |
| `log.Printf("order %d paid", id)` | `slog.Info("order paid", "order", id)` |
| `logrus.WithField("id", id)` | `logger.With("id", id)` |
| a zap logger threaded through every library | `*slog.Logger`; zap can still be the handler |
| writing to two loggers by hand | `slog.NewMultiHandler(a, b)` (1.26) |
| `runtime.ReadMemStats` (stops the world) | `runtime/metrics` |
| `log.SetFlags(...)` tweaks | `slog.SetDefault(logger)` |
