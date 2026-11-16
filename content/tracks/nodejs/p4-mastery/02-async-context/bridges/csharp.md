---
from: csharp
status: draft
concept: "AsyncLocal and Activity"
from_way: "AsyncLocal<T> flows with the ExecutionContext across awaits, ILogger scopes add context, and System.Diagnostics.Activity carries trace context for OpenTelemetry."
native_way: "AsyncLocalStorage flows across awaits, a logger mixin or child logger adds context, and the OpenTelemetry SDK carries trace context, also on top of AsyncLocalStorage."
sensei_tip: "AsyncLocal<T> and AsyncLocalStorage are the same idea with nearly the same name. Activity.Current is to .NET what the active OpenTelemetry span is to Node.js."
---

| C# | Node.js |
| --- | --- |
| `AsyncLocal<T>` | `AsyncLocalStorage<T>` |
| `logger.BeginScope(new { RequestId })` | `als.run({ requestId }, fn)` + mixin |
| Serilog JSON | pino |
| `Activity.Current` | Active OpenTelemetry span |
| `DiagnosticSource` | `diagnostics_channel` |
| EventCounters | `perf_hooks`, prom-client |
