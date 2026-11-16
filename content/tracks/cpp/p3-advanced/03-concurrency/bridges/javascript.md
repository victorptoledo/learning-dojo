---
from: javascript
status: draft
concept: "Threads and concurrency"
from_way: "One thread with an event loop; Workers and SharedArrayBuffer with Atomics for real parallelism."
native_way: "Many threads share all memory; mutexes, condition variables and atomics coordinate them; futures carry results."
sensei_tip: "Every C++ thread is like a Worker that shares all memory by default, so everything shared must be protected."
---

| JavaScript / TypeScript | C++ |
| --- | --- |
| `new Worker("w.js")` | `std::jthread t(work);` |
| `postMessage` (copies) | shared memory + a mutex, or a queue |
| `Atomics.add(view, 0, 1)` | `counter.fetch_add(1)` |
| `Atomics.wait` / `notify` | `std::atomic::wait` / `notify_one` |
| `await promise` | `future.get()` (blocks) |
| no data races in JS | data races are undefined behaviour |
