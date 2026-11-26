---
from: javascript
status: draft
concept: "Async I/O and the event loop"
from_way: "Promises and async/await on Node's single-threaded event loop."
native_way: "Futures that do nothing until awaited, run by Tokio's multi-threaded scheduler."
sensei_tip: "A JavaScript promise starts running when created; a Rust future is lazy and runs only when awaited or spawned. Forgetting .await means the work never happens."
---

| JavaScript / TypeScript | Rust |
| --- | --- |
| async function f() | async fn f() |
| await p | fut.await |
| Promise.all([a, b]) | tokio::join!(a, b) |
| Promise.race | tokio::select! |
| setTimeout | tokio::time::sleep |
| worker_threads for CPU work | spawn_blocking |
