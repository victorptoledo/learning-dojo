---
from: go
status: draft
concept: "One thread, many responsibilities"
from_way: "Goroutines on all cores; blocking one rarely matters."
native_way: "A page has one main thread for scripts and rendering; Web Workers are the closest thing to goroutines, talking by messages."
sensei_tip: "Workers with postMessage are goroutines with channels: share nothing, pass messages, keep the main thread free."
---

| Go | In the browser |
| --- | --- |
| A goroutine | A Web Worker |
| A channel | `postMessage` / `onmessage` |
| `runtime.Gosched()` | `scheduler.yield()` |
| pprof | DevTools' Performance panel |
