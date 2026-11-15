---
from: javascript
status: draft
concept: "Multithreading and synchronisation"
from_way: "One event loop per process, worker threads that share almost nothing, and message passing."
native_way: "Threads that share memory safely, plus channels for message passing."
sensei_tip: "Worker postMessage is a Rust channel. Rust also lets threads share memory, but only through Arc and Mutex, so the compiler stops you before a race can happen."
---

| JavaScript / TypeScript | Rust |
| --- | --- |
| new Worker(...) | thread::spawn |
| postMessage / onmessage | mpsc::channel |
| Atomics | std::sync::atomic |
| Promise.all over workers | thread::scope with several spawns |
