---
from: javascript
status: draft
concept: "Concurrency theory in JavaScript"
from_way: "Assuming single-threaded means no race conditions."
native_way: "One event loop means no data races between callbacks, but async interleavings at every await still cause race conditions; SharedArrayBuffer with Atomics gives real shared memory and CAS between workers."
sensei_tip: "Look for \"check, await, then act\" sequences: another callback can run at the await. Hold a mutex (for example an async lock) or re-check after awaiting."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| `await` in the middle of logic | an interleaving point |
| `Atomics.compareExchange` | CAS on shared memory |
| `Atomics.wait` / `notify` | a futex-like wait |
| `SharedArrayBuffer` | memory shared between workers |
| `postMessage` | message passing between workers |
