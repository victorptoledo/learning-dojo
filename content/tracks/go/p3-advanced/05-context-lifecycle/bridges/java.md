---
from: java
status: draft
concept: "Cancellation, timeouts and request scope"
from_way: "Thread.interrupt and InterruptedException, Future.cancel, CompletableFuture.orTimeout, ThreadLocal or MDC for request data, and Java 21+ structured concurrency and scoped values."
native_way: "One explicit context.Context passed as the first argument carries cancellation, the deadline and request-scoped values for the whole call tree."
sensei_tip: "Java's ScopedValue and StructuredTaskScope solve the same problem implicitly; Go makes it explicit in every signature, so you can see which calls can be cancelled."
---

| Java | Go |
| --- | --- |
| `thread.interrupt()` | `cancel()` |
| `catch (InterruptedException e)` | `case <-ctx.Done(): return ctx.Err()` |
| `future.get(2, SECONDS)` | `context.WithTimeout(ctx, 2*time.Second)` |
| `CompletableFuture.orTimeout(...)` | a deadline on the context passed down |
| `ThreadLocal` / MDC request id | `context.WithValue` with an unexported key |
| `StructuredTaskScope.ShutdownOnFailure` | `errgroup.WithContext` or `WithCancelCause` |
| JDBC `setQueryTimeout` | `db.QueryContext(ctx, ...)` |
| shutdown hook | `signal.NotifyContext` |
