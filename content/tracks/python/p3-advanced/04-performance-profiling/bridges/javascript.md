---
from: javascript
status: draft
concept: "Profiling an interpreter without V8's JIT"
from_way: "Chrome DevTools and node --prof or --cpu-prof, performance.now(), clinic.js flame graphs, V8's optimising JIT, and WebAssembly or native addons for hot code."
native_way: "time.perf_counter, timeit, cProfile and py-spy flame graphs, tracemalloc for memory, CPython's specialising interpreter, and NumPy, Cython or Rust extensions for hot code."
sensei_tip: "V8 optimises hot loops aggressively; CPython does much less, so a plain loop over a million numbers is far slower in Python. Reach for built-ins and NumPy instead of hand-written loops."
---

| Node.js | Python |
| --- | --- |
| `performance.now()` | `time.perf_counter()` |
| `node --cpu-prof` | `python -m cProfile -o out.prof` |
| clinic.js flame | `py-spy record -o profile.svg` |
| heap snapshots in DevTools | `tracemalloc`, `memray` |
| V8 TurboFan JIT | specialising interpreter, experimental JIT (3.13, upgraded in 3.15) |
| `array.join("")` | `"".join(parts)` |
| WebAssembly / N-API addons | NumPy, Cython, Rust with PyO3 |
