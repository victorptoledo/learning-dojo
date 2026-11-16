---
from: python
status: draft
concept: "Profiling and scaling a single-threaded runtime"
from_way: "cProfile and py-spy, tracemalloc for leaks, reference counting plus a cycle collector, gunicorn workers per core, Celery for background jobs."
native_way: "--cpu-prof and flame graphs, heap snapshots for leaks, a generational tracing GC, replicas or cluster per core, and BullMQ or another queue for background jobs."
sensei_tip: "The scaling story is the same as for a GIL-bound Python app: one process per core and a queue for slow work. The speed story differs: V8's JIT makes plain JavaScript loops far faster than CPython's."
---

| Python | Node.js |
| --- | --- |
| `py-spy record` | `--cpu-prof` / 0x |
| `tracemalloc` snapshots | Heap snapshots |
| gunicorn `-w 8` | 8 replicas or `cluster` |
| Celery + Redis | BullMQ + Redis |
| `functools.lru_cache` | lru-cache |
