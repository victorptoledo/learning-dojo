---
from: go
status: draft
concept: "Concurrency without threads in your code"
from_way: "Go runs goroutines on several OS threads with a scheduler; blocking calls look synchronous and the runtime parks the goroutine."
native_way: "Node.js runs all JavaScript on one thread; concurrency comes from async I/O and callbacks or promises, never from parallel execution of your code."
sensei_tip: "In Go a CPU-heavy goroutine only slows itself; in Node.js the same loop freezes the whole process. Go's net/http model and Node's event loop both serve thousands of connections, but only Go spreads your code over all cores by default."
---

| Go | Node.js |
| --- | --- |
| Goroutines on many OS threads | One main thread + async I/O |
| netpoller | libuv (epoll, kqueue, IOCP) |
| `go run main.go` | `node main.ts` |
| Static binary | The node runtime + your files |
| `GOMAXPROCS` cores by default | Several processes or worker threads for more cores |
