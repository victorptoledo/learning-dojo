---
from: python
status: draft
concept: "Callbacks registered by event name"
from_way: "Signals in Django or blinker, asyncio.Event for one-shot waits, or callback lists by hand."
native_way: "EventEmitter with named events, listeners called synchronously with arguments, events.once to await one event."
sensei_tip: "events.once(emitter, 'ready') is like await event.wait() on an asyncio.Event, with the error event wired in. And like Django signals, listeners run synchronously, so keep them short."
---

| Python | Node.js |
| --- | --- |
| `signal.connect(receiver)` | `emitter.on("name", listener)` |
| `signal.send(sender, **kw)` | `emitter.emit("name", ...args)` |
| `signal.disconnect(receiver)` | `emitter.off("name", listener)` |
| `await event.wait()` | `await once(emitter, "name")` |
| Async generator over a queue | `for await (const args of on(emitter, "name"))` |
