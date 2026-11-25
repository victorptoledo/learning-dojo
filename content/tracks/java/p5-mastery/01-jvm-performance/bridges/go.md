---
from: go
status: draft
concept: "The runtime under load"
from_way: "A non-moving concurrent GC tuned with GOGC and GOMEMLIMIT, and pprof."
native_way: "Generational, compacting collectors with many options, and JFR."
sensei_tip: "GOMEMLIMIT is close to -XX:MaxRAMPercentage; GOGC to the heap-sizing heuristics. JFR is the always-on, low-overhead equivalent of pprof endpoints."
---

| Go | Java 25 |
| --- | --- |
| pprof | JFR, async-profiler |
| GOGC | heap sizing and GC choice |
| GOMEMLIMIT | -XX:MaxRAMPercentage |
| runtime.ReadMemStats | jcmd GC.heap_info |
| go tool trace | JDK Mission Control |
