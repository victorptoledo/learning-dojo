---
from: javascript
status: draft
concept: "Promises, streams and races"
from_way: "Promise.race for the first result or a timeout, async iterators and streams to pass values along, p-limit style helpers to bound concurrency, and events for signals."
native_way: "select races channel operations, channels carry streams of values between goroutines, a buffered channel bounds concurrency and closing a channel broadcasts an event."
sensei_tip: "Promise.race([work, timeout]) leaves the losing promise running. In Go the loser keeps running too, unless you cancel it with a context: always pair select with cancellation."
---

| JavaScript | Go |
| --- | --- |
| `Promise.race([a, b])` | `select { case v := <-a: ... case v := <-b: ... }` |
| timeout promise with `setTimeout` | `case <-time.After(d):` |
| `for await (const v of stream)` | `for v := range ch` |
| `stream.end()` | `close(ch)` |
| p-limit(3) | `make(chan struct{}, 3)` |
| `EventEmitter.emit("done")` to all listeners | `close(done)` wakes every receiver |
| `AbortSignal` | `ctx.Done()` |
