---
from: csharp
status: draft
concept: "ILogger, Serilog and OpenTelemetry"
from_way: "ILogger<T> from dependency injection with message templates, BeginScope for request data, Serilog sinks and enrichers, and System.Diagnostics.Metrics with OpenTelemetry exporters."
native_way: "*slog.Logger passed explicitly, key-value attributes instead of message templates, With instead of BeginScope, handlers instead of sinks, and the OpenTelemetry Go SDK."
sensei_tip: "Message templates like \"Order {OrderId} paid\" become a fixed message and attributes: logger.Info(\"order paid\", \"order_id\", id). Fixed messages are easier to search."
---

| C# | Go |
| --- | --- |
| `ILogger<OrderService>` injected | `*slog.Logger` passed to `NewOrderService` |
| `log.LogInformation("Order {Id} paid", id)` | `logger.Info("order paid", "id", id)` |
| `using (log.BeginScope(new { RequestId }))` | `logger.With("request_id", id)` |
| Serilog JSON sink | `slog.NewJSONHandler(os.Stdout, opts)` |
| Serilog destructuring policies | `slog.LogValuer` |
| `Meter.CreateCounter<long>` | OpenTelemetry meter or Prometheus counter |
| health checks middleware | `/healthz` and `/readyz` handlers |
