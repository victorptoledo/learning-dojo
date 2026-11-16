---
from: go
status: draft
concept: "Threads and synchronisation"
from_way: "Goroutines on a scheduler, channels and select, sync.Mutex, sync.WaitGroup, atomics, context and the race detector."
native_way: "OS threads (std::jthread), mutexes and condition variables, std::atomic, futures and stop tokens; ThreadSanitizer is the race detector."
sensei_tip: "There are no channels in the standard library: a mutex plus a condition variable around a deque is your buffered channel, and close() is channel close."
---

| Go | C++ |
| --- | --- |
| `go f()` | `std::jthread t(f);` (a real OS thread) |
| `ch := make(chan T, n)` | a blocking queue with capacity n |
| `close(ch)` + `for v := range ch` | `queue.close()` + `while (auto v = queue.pop())` |
| `sync.Mutex` + `defer Unlock` | `std::mutex` + `std::lock_guard` |
| `sync.WaitGroup` | `std::latch`, or joining jthreads |
| `ctx.Done()` | `std::stop_token` |
| `go test -race` | `-fsanitize=thread` |
