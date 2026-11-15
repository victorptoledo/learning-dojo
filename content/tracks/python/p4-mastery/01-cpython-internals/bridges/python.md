---
from: python
status: draft
concept: "The interpreter has changed under you"
from_way: "Assuming the 'Python is slow, frames are heavy' interpreter of 3.10 and earlier, the GIL as a permanent fact, sys.settrace-based tools, and one GIL per process."
native_way: "Cheap frames and a specialising interpreter (3.11), immortal objects and per-interpreter GILs (3.12), an experimental JIT and free-threaded build (3.13), and sys.monitoring for tools."
sensei_tip: "Advice written before 3.11 about function-call overhead or attribute lookups is often outdated; check it with dis and timeit on your own version before applying it."
---

| Older assumption | Python 3.15 |
| --- | --- |
| frames are expensive Python objects | lightweight internal frames, created lazily (3.11) |
| the same bytecode runs for every type | specialised instructions, visible with `dis.dis(f, adaptive=True)` |
| one GIL per process, forever | per-interpreter GILs (3.12), free-threaded build (3.13t, supported since 3.14, stable ABI in 3.15) |
| `None` and small ints have changing refcounts | immortal objects (3.12) |
| coverage and debuggers slow everything | `sys.monitoring` (3.12) |
| no JIT in CPython | an experimental copy-and-patch JIT (3.13, upgraded in 3.15) |
