---
from: go
status: draft
concept: "Events instead of channels"
from_way: "Channels carry values between goroutines; select waits for several, and closing a channel signals the end."
native_way: "EventEmitter delivers named events to listeners on the same thread; events.on turns them into an async iterator, and events.once into a promise."
sensei_tip: "Go has no built-in emitter because channels cover it. In Node.js, for await over events.on(emitter, 'msg') is the closest thing to ranging over a channel, with the signal playing ctx.Done()."
---

| Go | Node.js |
| --- | --- |
| `ch <- v` | `emitter.emit("v", v)` |
| `v := <-ch` | `const [v] = await once(emitter, "v")` |
| `for v := range ch` | `for await (const [v] of on(emitter, "v", { signal }))` |
| `select` with `ctx.Done()` | `once(..., { signal })` |
| Panic on send to a closed channel | Throw on an unhandled error event |
