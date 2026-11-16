---
from: java
status: draft
concept: "Observers built into the platform"
from_way: "The observer pattern by hand: listener interfaces, addXListener/removeXListener, or an event bus such as Guava EventBus or Spring ApplicationEventPublisher."
native_way: "EventEmitter is built in and used by the whole standard library: named events, on/once/off, emit with arguments, and a special error event."
sensei_tip: "Spring's synchronous @EventListener is the closest match: listeners run on the publisher's thread. Unlike Java listeners, an unhandled error event throws, so a missing listener can stop the process."
---

| Java | Node.js |
| --- | --- |
| `addActionListener(l)` | `emitter.on("action", l)` |
| `removeActionListener(l)` | `emitter.off("action", l)` |
| `publisher.publishEvent(e)` | `emitter.emit("name", ...args)` |
| Listener interface types | Event map: `EventEmitter<{ name: [args] }>` |
| `CompletableFuture` for one event | `events.once(emitter, "name")` |
