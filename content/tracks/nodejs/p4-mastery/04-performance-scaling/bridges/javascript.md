---
from: javascript
status: draft
concept: "Performance on the server"
from_way: "Front-end performance is about bundle size, rendering, Core Web Vitals and the Performance panel of DevTools."
native_way: "Server performance is about throughput, latency percentiles under load, the event loop, heap and GC, measured with load tests, --cpu-prof and heap snapshots, in the same DevTools."
sensei_tip: "You already know the tools: DevTools' profiler and memory panel work on Node.js through --inspect. What is new is load: a leak of 1 KB per request is nothing in a tab and an outage on a server."
---

| Front end | Node.js server |
| --- | --- |
| Lighthouse | autocannon, k6 |
| Performance panel | `--cpu-prof`, `--inspect` |
| Memory panel | Heap snapshots of the server |
| Long tasks | Event loop delay |
| CDN caching | Caches, replicas, queues |
