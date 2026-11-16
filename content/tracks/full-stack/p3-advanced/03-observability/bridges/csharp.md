---
from: csharp
status: reviewed
concept: "OpenTelemetry is built into .NET"
from_way: "Text logs written by a file logger, read over remote desktop."
native_way: "ILogger with structured templates exported through OpenTelemetry, Activity as the span with W3C propagation by default, System.Diagnostics.Metrics histograms."
sensei_tip: "In .NET, Activity.Current is the current span and HttpClient propagates traceparent automatically; your job is mostly exporting it."
---

| .NET | Observability |
| --- | --- |
| `logger.LogInformation("Order {OrderId}", id)` | structured logs |
| `Activity` | a span, W3C Trace Context by default |
| `Meter.CreateHistogram<double>` | latency histograms |
| `AddOpenTelemetry()` | one exporter for logs, metrics and traces |
