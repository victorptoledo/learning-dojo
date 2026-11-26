---
from: python
status: draft
concept: "Async I/O and the event loop"
from_way: "asyncio with async def and await, one event loop per thread."
native_way: "async fn and .await on futures driven by a runtime such as Tokio, on many threads."
sensei_tip: "The rule is the same as in asyncio: never block inside async code. time.sleep in a coroutine blocks the loop, and std::thread::sleep blocks a Tokio worker; use tokio::time::sleep."
---

| Python | Rust |
| --- | --- |
| async def f() | async fn f() |
| await f() | f().await |
| asyncio.run(main()) | #[tokio::main] |
| asyncio.gather(a, b) | tokio::join!(a, b) |
| asyncio.create_task | tokio::spawn |
| run_in_executor | tokio::task::spawn_blocking |
