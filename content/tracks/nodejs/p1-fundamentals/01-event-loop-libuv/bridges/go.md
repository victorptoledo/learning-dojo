---
from: go
status: draft
concept: "A scheduler you cannot see vs a loop you must respect"
from_way: "The Go scheduler preempts goroutines and spreads them over GOMAXPROCS threads; a busy goroutine does not stop the others."
native_way: "The event loop never preempts: each callback runs to the end, and only then can the next one run."
sensei_tip: "Go has had asynchronous preemption since 1.14, so a tight loop cannot starve other goroutines. Node.js has nothing like it: yield by hand with setImmediate or move the work to a worker."
---

| Go | Node.js |
| --- | --- |
| Goroutine scheduler with preemption | Run-to-completion callbacks |
| `runtime.Gosched()` | `await setImmediate()` |
| netpoller | libuv poll phase |
| Blocking syscalls get their own thread | fs and crypto use the libuv thread pool |
| `GODEBUG=schedtrace` | `monitorEventLoopDelay`, `--cpu-prof` |
