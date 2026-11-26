---
from: javascript
status: draft
concept: "Promises and the event loop"
from_way: "A `Promise` starts running when created; the event loop runs its callbacks from the microtask queue."
native_way: "A Rust future does nothing until polled, and the executor polls it again only when woken."
sensei_tip: "`new Promise(resolve => ...)` is eager; `async { ... }` in Rust is lazy: nothing happens until something polls it."
---

| JavaScript | Rust |
| --- | --- |
| `new Promise(...)` (eager) | a future (lazy until polled) |
| `resolve(value)` | `Poll::Ready(value)` on the next poll after `wake()` |
| `Promise.all` | `join_all` |
| `Promise.race` with a timer | `timeout` |
