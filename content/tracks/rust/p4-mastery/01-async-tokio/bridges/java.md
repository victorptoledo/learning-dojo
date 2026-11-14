---
from: java
status: draft
concept: "Async I/O and the event loop"
from_way: "Heavy OS threads, `CompletableFuture` or virtual threads."
native_way: "Rust futures are lazy (they do nothing until awaited) and need no hidden allocation; runtimes such as Tokio schedule thousands of light tasks."
sensei_tip: "Cancelling an async task in Rust is as simple as dropping its future."
---
