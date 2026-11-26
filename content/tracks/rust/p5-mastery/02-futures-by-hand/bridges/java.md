---
from: java
status: draft
concept: "How async work resumes"
from_way: "`CompletableFuture` callbacks run on an executor; virtual threads park and resume a stack."
native_way: "A `Future` is polled; it returns `Pending` and keeps a `Waker` to call when it can progress."
sensei_tip: "A virtual thread parks a whole stack; a Rust future is a state machine with no stack of its own, which is why it needs `Pin`."
---

| Java | Rust |
| --- | --- |
| `CompletableFuture.thenApply` | `async` block with `.await` |
| `ExecutorService` | an executor: `block_on`, Tokio's runtime |
| `LockSupport.park` / `unpark` | `thread::park` / `Thread::unpark` in a waker |
| `future.cancel(true)` | drop the future |
