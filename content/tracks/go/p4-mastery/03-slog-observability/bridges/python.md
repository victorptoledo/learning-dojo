---
from: python
status: draft
concept: "logging, structlog and metrics"
from_way: "The logging module with module loggers and dictConfig, structlog or python-json-logger for JSON, LoggerAdapter or contextvars for request data, and prometheus_client."
native_way: "log/slog with one configured logger, JSON or text handlers, With for request attributes, typed attributes, and Prometheus or OpenTelemetry clients."
sensei_tip: "structlog's bind() is slog's With(): both return a new logger carrying the extra keys, and neither mutates the original."
---

| Python | Go |
| --- | --- |
| `logging.getLogger(__name__)` | a `*slog.Logger` field, or `slog.Default()` |
| `log.info("paid %s", order_id)` | `logger.Info("paid", "order", orderID)` |
| `structlog.get_logger().bind(request_id=id)` | `logger.With("request_id", id)` |
| `python-json-logger` | `slog.NewJSONHandler` |
| `log.exception("failed")` | `logger.Error("failed", "err", err)` |
| `logging.Filter` that masks secrets | `slog.LogValuer` on the secret type |
| `prometheus_client.Counter` | `prometheus/client_golang` counters |
| `dictConfig` levels | `slog.HandlerOptions{Level: levelVar}` |
