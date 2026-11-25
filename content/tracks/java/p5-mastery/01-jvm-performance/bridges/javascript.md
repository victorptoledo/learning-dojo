---
from: javascript
status: draft
concept: "The runtime under load"
from_way: "V8 with its JIT and garbage collector, profiled with --inspect and DevTools."
native_way: "The HotSpot JIT and collectors such as G1 and ZGC, profiled with JFR."
sensei_tip: "V8 and HotSpot work alike: interpreter first, then optimised code for hot paths. --max-old-space-size is the JVM's -Xmx or MaxRAMPercentage."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| --max-old-space-size | -Xmx or -XX:MaxRAMPercentage |
| DevTools CPU profile | JFR / JDK Mission Control |
| heap snapshot | jcmd GC.heap_dump |
| --trace-gc | -Xlog:gc |
