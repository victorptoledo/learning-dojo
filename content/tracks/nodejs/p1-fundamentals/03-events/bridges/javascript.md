---
from: javascript
status: draft
concept: "EventEmitter next to EventTarget"
from_way: "addEventListener on DOM elements, Event objects with type and detail, listeners that never throw into dispatchEvent."
native_way: "EventEmitter with on/once/off and emit(name, ...args), listeners get plain arguments, and an unhandled error event throws."
sensei_tip: "Both run listeners synchronously. The difference that bites is error: dispatching an error Event in the browser is harmless, emitting error in Node.js without a listener crashes."
---

| Browser `EventTarget` | Node.js `EventEmitter` |
| --- | --- |
| `addEventListener("x", f)` | `on("x", f)` |
| `{ once: true }` | `once("x", f)` |
| `dispatchEvent(new CustomEvent("x", { detail }))` | `emit("x", detail)` |
| `removeEventListener` | `off` |
| `{ signal }` option | `events.once` / `events.on` with `{ signal }` |
