---
from: python
status: draft
concept: "Processes and threads in Python"
from_way: "Using threads to speed up CPU-bound Python code."
native_way: "Threads are OS threads, but classic CPython's GIL lets only one run Python bytecode at a time; multiprocessing uses separate processes; asyncio multiplexes coroutines on one thread; free-threaded builds (3.13+) remove the GIL."
sensei_tip: "Threads or asyncio for I/O, multiprocessing or concurrent.futures.ProcessPoolExecutor for CPU work, unless you run a free-threaded build."
---

| In Python | Underneath |
| --- | --- |
| `threading.Thread` | an OS thread, serialised by the GIL |
| `multiprocessing.Process` | a separate process (fork or spawn) |
| `asyncio.create_task` | a coroutine on the event loop |
| `os.fork()` | the system call itself |
| `os.nice(10)` | lower scheduling priority |
