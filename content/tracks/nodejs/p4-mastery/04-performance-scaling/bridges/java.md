---
from: java
status: draft
concept: "JIT, GC and scaling"
from_way: "HotSpot JIT with tiered compilation, -Xmx and container-aware heap sizing, generational collectors (G1, ZGC), JFR and async-profiler, heap dumps in Eclipse MAT, and one JVM using all cores."
native_way: "V8 JIT with hidden classes and deoptimisation, --max-old-space-size, a generational collector, --cpu-prof and DevTools, heap snapshots in DevTools, and one process per core."
sensei_tip: "The concepts carry over: profile before tuning, compare heap dumps, size the heap below the container. The big difference is scaling: a JVM fills all cores with threads; Node.js needs more processes."
---

| Java | Node.js |
| --- | --- |
| `-Xmx768m` | `--max-old-space-size=768` |
| JFR / async-profiler | `--cpu-prof`, Clinic.js |
| Heap dump + Eclipse MAT | Heap snapshot + DevTools |
| JMH | mitata, careful `performance.now()` |
| Thread pool sizing | Replicas / `cluster` |
| Caffeine cache | lru-cache |
