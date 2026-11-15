---
from: python
status: draft
concept: "The runtime under load"
from_way: "cProfile and py-spy, reference counting, and the GIL."
native_way: "A JIT-compiled runtime with tunable garbage collectors, profiled with JFR and jcmd."
sensei_tip: "The JVM is slow for the first seconds while the JIT warms up, then usually much faster than CPython. Measure after warm-up, with JMH for micro-benchmarks."
---

| Python | Java 25 |
| --- | --- |
| cProfile / py-spy | JFR, async-profiler |
| tracemalloc | heap histogram (jcmd GC.class_histogram) |
| gc module | G1, ZGC, -Xlog:gc |
| python -X importtime | -Xlog:class+load, the AOT cache |
