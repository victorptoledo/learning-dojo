---
from: java
status: draft
concept: "Observing every layer from Java"
from_way: "Profiling only the Java code."
native_way: "JDK Flight Recorder captures GC, allocation, locks, threads, I/O and method samples with low overhead; async-profiler adds native frames and hardware counters; together they cover most layers in one recording."
sensei_tip: "Start investigations with `jcmd <pid> JFR.start duration=60s filename=rec.jfr` and open it in JDK Mission Control before changing any code."
---

| In Java | Underneath |
| --- | --- |
| JFR | GC, locks, I/O, samples in one file |
| async-profiler | CPU and allocation flame graphs, native frames |
| `jcmd Thread.print` | thread dump: who waits on what |
| Micrometer percentiles | p99, not averages |
| `-Xlog:gc*` | GC pauses, a common tail |
