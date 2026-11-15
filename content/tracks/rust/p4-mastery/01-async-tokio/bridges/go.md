---
from: go
status: draft
concept: "Async I/O and the event loop"
from_way: "Goroutines that block freely, scheduled by the Go runtime."
native_way: "async functions that yield at each .await, scheduled by Tokio."
sensei_tip: "Go hides async: every goroutine can block and the runtime parks it. In Rust you mark async code and must not block in it; in exchange tasks are tiny and the cost is visible."
---

| Go | Rust |
| --- | --- |
| go f() | tokio::spawn(f()) |
| blocking call in a goroutine | .await on an async call |
| select { case ... } | tokio::select! |
| time.Sleep | tokio::time::sleep(...).await |
| context.WithTimeout | tokio::time::timeout |
