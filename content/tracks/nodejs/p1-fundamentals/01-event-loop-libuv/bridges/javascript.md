---
from: javascript
status: draft
concept: "The event loop, server edition"
from_way: "The browser loop runs tasks, then microtasks, then renders; a long task freezes one tab."
native_way: "Node's loop has libuv phases (timers, poll, check, close), process.nextTick before microtasks, and setImmediate; a long task freezes every request of the server."
sensei_tip: "Promises and microtasks behave as in the browser. What is new is process.nextTick (runs before promises), setImmediate (the check phase) and the thread pool behind fs and crypto."
---

| Browser | Node.js |
| --- | --- |
| Task queue | Phases: timers, poll, check, close |
| Microtasks | nextTick queue, then microtasks |
| `requestAnimationFrame` | (none) |
| `setTimeout(f, 0)` | Also at least 1 ms |
| A long task freezes a tab | A long task freezes the whole server |
