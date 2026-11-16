---
from: python
status: draft
concept: "Parallel CPU work"
from_way: "The GIL keeps threads from running Python in parallel (unless free-threaded builds), so CPU work goes to multiprocessing or ProcessPoolExecutor, with pickled arguments."
native_way: "Worker threads run JavaScript in parallel inside one process, each with its own heap; arguments are structured-cloned or transferred."
sensei_tip: "A worker pool is ProcessPoolExecutor with cheaper workers: still no shared objects and still a copy per message, but no separate process to start."
---

| Python | Node.js |
| --- | --- |
| `ProcessPoolExecutor` | A worker pool |
| `executor.submit(f, x)` | `pool.run(x)` |
| Pickling arguments | Structured clone |
| `multiprocessing.shared_memory` | `SharedArrayBuffer` |
| `os.cpu_count()` | `os.availableParallelism()` |
