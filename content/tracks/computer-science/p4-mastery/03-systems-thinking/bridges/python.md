---
from: python
status: draft
concept: "Observing every layer from Python"
from_way: "Using print timing to find a bottleneck."
native_way: "cProfile measures function calls; py-spy samples a running process without changes (including native frames); tracemalloc follows allocations; the OS tools (strace, perf) see the rest."
sensei_tip: "Attach `py-spy top --pid <pid>` to the slow process first: it shows where time goes right now, without a restart."
---

| In Python | Underneath |
| --- | --- |
| `cProfile` / `pstats` | deterministic function profile |
| `py-spy` | a sampling profiler for live processes |
| `tracemalloc` | who allocated the memory |
| `time.perf_counter_ns` | a monotonic clock for measurements |
| `perf` with Python 3.12+ support | native and Python frames together |
