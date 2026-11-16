---
from: java
status: draft
concept: "Async I/O"
from_way: "CompletableFuture, HttpClient.sendAsync and, since Java 21, virtual threads that make blocking cheap."
native_way: "Promises and async/await on a single main thread; fetch is non-blocking, and AbortController cancels it."
sensei_tip: "There are no virtual threads in a page: blocking the main thread freezes the UI. Treat every Promise as a CompletableFuture you must join or handle."
---

| Java | In the browser |
| --- | --- |
| `HttpClient.sendAsync(req, ofString())` | `fetch(url)` |
| `CompletableFuture.allOf(...)` | `Promise.all([...])` |
| `future.cancel(true)` | `controller.abort()` |
| `response.statusCode() >= 400` | `!response.ok` |
