---
from: java
status: draft
concept: "Futures as the common language of async code"
from_way: "Blocking calls by default, CompletableFuture chains (thenCompose, allOf) for async code, and virtual threads to make blocking cheap again."
native_way: "Callbacks in older APIs, promises everywhere else, and async/await that reads like blocking code without blocking the thread."
sensei_tip: "async/await gives you what virtual threads give Java: straight-line code. But a forgotten await does not block, it silently runs in the background, so treat floating promises as bugs."
---

| Java | Node.js |
| --- | --- |
| `CompletableFuture<T>` | `Promise<T>` |
| `thenCompose` / `thenApply` | `await` / `.then` |
| `CompletableFuture.allOf` | `Promise.all` |
| `future.get(2, SECONDS)` | `AbortSignal.timeout(2000)` |
| `Future.cancel(true)` | `controller.abort()` |
| Uncaught exception in a thread | Unhandled rejection ends the process |
