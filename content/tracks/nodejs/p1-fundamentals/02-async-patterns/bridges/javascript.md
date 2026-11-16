---
from: javascript
status: draft
concept: "Node's own async conventions"
from_way: "Promises and async/await everywhere, fetch with AbortController, and unhandled rejections only logged in the console."
native_way: "The same, plus error-first callbacks in older APIs, util.promisify, the node:*/promises modules, and a process that stops on an unhandled rejection."
sensei_tip: "The language is identical; the stakes are not. In a browser an unhandled rejection is a red line in the console, in Node.js it takes the server down."
---

| Browser habit | Node.js |
| --- | --- |
| Callbacks with `(event)` | Error-first callbacks `(err, value)` |
| `fetch` + `AbortController` | The same, plus `AbortSignal` in fs, timers and streams |
| `new Promise(r => setTimeout(r, ms))` | `setTimeout` from `node:timers/promises` |
| Unhandled rejection: a console error | Unhandled rejection: the process exits |
