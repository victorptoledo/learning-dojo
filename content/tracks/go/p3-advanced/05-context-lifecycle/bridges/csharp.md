---
from: csharp
status: draft
concept: "CancellationToken"
from_way: "A CancellationToken parameter on async methods, CancellationTokenSource with CancelAfter, linked token sources, OperationCanceledException, and AsyncLocal or HttpContext.Items for request data."
native_way: "context.Context is a CancellationToken that also carries a deadline and request-scoped values, passed as the first parameter instead of the last."
sensei_tip: "CreateLinkedTokenSource is what every derived Go context does by default: a child is always linked to its parent."
---

| C# | Go |
| --- | --- |
| `CancellationToken ct` (last parameter) | `ctx context.Context` (first parameter) |
| `new CancellationTokenSource()` / `Cancel()` | `context.WithCancel` / `cancel()` |
| `cts.CancelAfter(2000)` | `context.WithTimeout(ctx, 2*time.Second)` |
| `CreateLinkedTokenSource(a)` | any context derived from a parent |
| `ct.ThrowIfCancellationRequested()` | `if err := ctx.Err(); err != nil { return err }` |
| `catch (OperationCanceledException)` | `errors.Is(err, context.Canceled)` |
| `ct.Register(callback)` | `context.AfterFunc(ctx, f)` |
| `HttpContext.RequestAborted` | `r.Context()` |
