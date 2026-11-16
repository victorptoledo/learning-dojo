---
from: python
status: draft
concept: "Threads and concurrency"
from_way: "threading with the GIL (optional in free-threaded 3.13+), Lock, Condition, queue.Queue and concurrent.futures."
native_way: "Threads run truly in parallel; std::mutex, std::condition_variable and std::atomic synchronise them; std::future gets results."
sensei_tip: "With no GIL, every shared write needs synchronisation. queue.Queue is the blocking queue you build in the kata."
---

| Python | C++ |
| --- | --- |
| `threading.Thread(target=f).start()` | `std::jthread t(f);` |
| `with lock:` | `std::lock_guard g(m);` |
| `threading.Condition` + `wait_for` | `std::condition_variable::wait(lock, pred)` |
| `queue.Queue(maxsize)` | a blocking queue (mutex + condition variables) |
| `ThreadPoolExecutor.submit(f)` | a worker pool returning `std::future` |
| `threading.Event` | `std::atomic<bool>` with wait/notify, or `std::stop_token` |
