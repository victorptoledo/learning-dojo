---
from: csharp
status: draft
concept: "API failure modes in production"
from_way: "`HttpClient` with the default 100-second timeout and Polly retries stacked on gateway retries."
native_way: "`AddStandardResilienceHandler` (timeouts, retries with jitter, circuit breaker) in one layer, `IExceptionHandler` for safe 500s, OpenTelemetry metrics per client."
sensei_tip: "The standard resilience handler already combines total timeout, retry, circuit breaker and attempt timeout in the right order; do not add another retry layer on top."
---

| C# / ASP.NET Core | Production concern |
| --- | --- |
| `AddStandardResilienceHandler` | timeouts, retries, circuit breaker |
| `IExceptionHandler` | safe 500s |
| OpenTelemetry .NET | traces and correlation ids |
| rate limiter partitions | protect from one client |
| `Activity.Current.TraceId` in problems | correlation id |
