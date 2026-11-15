---
from: javascript
status: draft
concept: "The event loop, promises and workers"
from_way: "One thread with an event loop, async functions and await, Promise.all for fan-out, and worker threads for CPU-bound work with message passing."
native_way: "Many goroutines scheduled across all cores, blocking calls that park only the goroutine, WaitGroup or channels for fan-out, and shared memory protected by sync when needed."
sensei_tip: "In Node only one callback runs at a time, so shared variables are safe by accident. In Go two goroutines really run at once: run your tests with -race from day one."
---

| JavaScript / Node | Go |
| --- | --- |
| `fetch(url)` returning a Promise | `go fetch(url)` plus a channel for the result |
| `await Promise.all(urls.map(fetch))` | `wg.Go` per URL, then `wg.Wait()` |
| `async` / `await` | ordinary blocking calls |
| event loop (one thread) | G-M-P scheduler on GOMAXPROCS threads |
| `new Worker("cpu.js")` | another goroutine |
| `AbortController` | `context.WithCancel` |
| unhandled rejection | an unrecovered panic crashes the program |
