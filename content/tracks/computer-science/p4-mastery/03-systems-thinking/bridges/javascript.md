---
from: javascript
status: draft
concept: "Observing every layer from Node.js"
from_way: "Guessing whether the event loop is blocked."
native_way: "Node exposes event-loop delay (perf_hooks.monitorEventLoopDelay), CPU profiles (--cpu-prof), heap snapshots and diagnostic reports; browsers have the Performance panel and long-task API."
sensei_tip: "Measure event-loop delay percentiles in production: a high p99 there means CPU work blocking every request."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| `monitorEventLoopDelay()` | how long callbacks wait |
| `--cpu-prof` | a CPU profile file |
| `--heapsnapshot-near-heap-limit` | memory at the edge |
| `process.report` | a diagnostic dump |
| clinic.js / 0x | flame graphs |
