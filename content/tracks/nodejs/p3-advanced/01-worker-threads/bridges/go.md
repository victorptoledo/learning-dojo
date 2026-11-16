---
from: go
status: draft
concept: "Parallelism by message passing"
from_way: "Goroutines run in parallel on all cores by default and share memory, protected by channels or mutexes."
native_way: "Only workers run in parallel with the main thread; they share no objects and communicate with messages, like goroutines that only use channels."
sensei_tip: "Go's proverb \"share memory by communicating\" is enforced in Node.js: postMessage is the channel. What Go gives for free (all cores, cheap goroutines), Node.js needs a pool of a few heavy workers for."
---

| Go | Node.js |
| --- | --- |
| `go work(x)` | `pool.run(x)` |
| Channels | `postMessage` / `MessagePort` |
| `sync/atomic` | `Atomics` on a `SharedArrayBuffer` |
| Worker pool with a buffered channel | A worker pool with a queue |
| `runtime.NumCPU()` | `os.availableParallelism()` |
