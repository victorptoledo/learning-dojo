---
from: java
status: draft
concept: "CompletableFuture, checked exceptions and virtual threads"
from_way: "Blocking code on virtual threads or CompletableFuture chains; checked exceptions declared with throws; ExecutorService for concurrency limits."
native_way: "async/await on one event-loop thread; no checked exceptions (catch gives unknown); Promise.all and small worker pools for concurrency; AbortSignal for cancellation."
sensei_tip: "There are no checked exceptions in TypeScript. If a failure is part of the contract, make it a Result union in the return type; that is the honest replacement for throws."
---

| Java | TypeScript |
| --- | --- |
| `CompletableFuture<User>` | `Promise<User>` |
| `future.join()` | `await promise` |
| `CompletableFuture.allOf(...)` | `Promise.all([...])` |
| `throws NotFoundException` | `Result<User, "not-found">` or a documented exception |
| `catch (IOException e)` typed | `catch (error)` is `unknown`, narrow with `instanceof` |
| `try (var r = open())` | `using r = open()` |
| `Future.cancel(true)` | `AbortController.abort()` |
