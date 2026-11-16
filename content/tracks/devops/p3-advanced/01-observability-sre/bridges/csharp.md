---
from: csharp
status: draft
concept: "Emit metrics, structured logs and traces from the service"
from_way: "Performance counters and Event Log entries, viewed on the server."
native_way: ".NET's built-in System.Diagnostics Activity and Meter APIs map directly to OpenTelemetry traces and metrics, and ILogger with JSON output carries the trace ID."
sensei_tip: "An Activity is an OpenTelemetry span: add the OpenTelemetry.Extensions.Hosting package and the traces you already create get exported."
---

| C# world | Observability |
| --- | --- |
| performance counters | `Meter` metrics |
| `Activity` | a span |
| `ILogger` scopes | structured logs with trace context |
| Application Insights | an OpenTelemetry backend |
