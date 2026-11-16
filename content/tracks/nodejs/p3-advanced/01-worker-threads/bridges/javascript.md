---
from: javascript
status: draft
concept: "Web Workers on the server"
from_way: "Web Workers with new Worker(url), postMessage and onmessage, transferable objects, and SharedArrayBuffer behind cross-origin isolation."
native_way: "worker_threads with new Worker(url), parentPort.postMessage and on(\"message\"), the same transfer lists and SharedArrayBuffer, plus workerData and resource limits."
sensei_tip: "The model is the one you know from Web Workers. The names differ (parentPort instead of self, EventEmitter instead of onmessage), and the reason is new: on a server a blocked thread blocks every user."
---

| Web Workers | worker_threads |
| --- | --- |
| `new Worker("w.js", { type: "module" })` | `new Worker(new URL("./w.ts", import.meta.url))` |
| `self.postMessage(x)` | `parentPort.postMessage(x)` |
| `self.onmessage = (e) => e.data` | `parentPort.on("message", (data) => ...)` |
| `worker.postMessage(x, [buf])` | The same |
| `worker.terminate()` | The same (returns a promise) |
