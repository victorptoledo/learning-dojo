---
from: javascript
status: draft
concept: "Rendering is part of the loop"
from_way: "Node.js's event loop: timers, I/O callbacks and microtasks, with no rendering at all."
native_way: "In the page, the loop also renders: after tasks and microtasks, the browser may run rAF callbacks, style, layout and paint."
sensei_tip: "A tight loop that is harmless in a Node script freezes a page; profile with the Performance panel, not console.time alone."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| `setImmediate` | `setTimeout(fn, 0)` or `scheduler.yield()` |
| `worker_threads` | Web Workers |
| `--prof` CPU profiles | DevTools' Performance panel |
| No rendering step | rAF → style → layout → paint every frame |
