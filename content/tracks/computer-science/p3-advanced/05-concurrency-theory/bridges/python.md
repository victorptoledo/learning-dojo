---
from: python
status: draft
concept: "Concurrency theory in Python"
from_way: "Believing the GIL makes Python code thread safe."
native_way: "The GIL makes single bytecodes atomic, but `count += 1` is several bytecodes and can still lose updates; free-threaded CPython removes even that; threading provides locks, conditions, semaphores and events."
sensei_tip: "Protect shared state with threading.Lock (with blocks), use queue.Queue to hand work between threads, and use asyncio.Lock in coroutines."
---

| In Python | Underneath |
| --- | --- |
| `count += 1` in threads | a race, GIL or not |
| `threading.Lock()` | a mutex |
| `threading.Condition` | a monitor's condition variable |
| `queue.Queue` | a thread-safe bounded buffer |
| `asyncio` tasks | interleave only at await points |
