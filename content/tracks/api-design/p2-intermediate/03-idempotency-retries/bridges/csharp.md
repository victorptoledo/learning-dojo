---
from: csharp
status: draft
concept: "Idempotency keys and safe retries"
from_way: "Polly retry policies on all `HttpClient` calls, including POSTs."
native_way: "`Microsoft.Extensions.Http.Resilience` with a retry strategy filtered to transient failures, a `Guid` key per operation, and a unique index on the server."
sensei_tip: "The standard resilience handler retries every method by default; call `options.Retry.DisableForUnsafeHttpMethods()` and retry POSTs only when they carry an Idempotency-Key."
---

| C# / ASP.NET Core | Idempotency concept |
| --- | --- |
| `Guid.NewGuid()` | idempotency key |
| `request.Headers.Add("Idempotency-Key", key)` | sending it |
| `AddResilienceHandler` with `DelayBackoffType.Exponential`, `UseJitter = true` | backoff with jitter |
| unique index on `(ClientId, Key)` | atomic check |
| `DbUpdateException` on duplicate insert | the losing concurrent request |
