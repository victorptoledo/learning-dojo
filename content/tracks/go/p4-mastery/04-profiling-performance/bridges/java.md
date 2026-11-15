---
from: java
status: draft
concept: "JFR, async-profiler and GC tuning"
from_way: "Java Flight Recorder and JDK Mission Control, async-profiler flame graphs, heap dumps with Eclipse MAT, JMH benchmarks, and choosing and tuning a collector (G1, ZGC) with many -XX flags."
native_way: "pprof profiles from tests or net/http/pprof, the execution tracer and flight recorder, benchmarks with b.Loop and benchstat, and one collector with two knobs: GOGC and GOMEMLIMIT."
sensei_tip: "There is no collector to choose and almost nothing to tune. When Go uses too much memory, look for allocations and leaks in a heap profile before touching GOGC."
---

| Java | Go |
| --- | --- |
| async-profiler flame graph | `go tool pprof -http=:8080 cpu.out` |
| JFR recording | `runtime/trace`, `trace.FlightRecorder` |
| heap dump + MAT | heap profile (`inuse_space`), `debug.WriteHeapDump` |
| thread dump | goroutine profile |
| JMH | `testing.B` + benchstat |
| `-XX:+UseG1GC`, `-XX:MaxGCPauseMillis` | one concurrent GC; `GOGC` |
| `-Xmx` | `GOMEMLIMIT` (a soft limit) |
| JIT warm-up and C2 | ahead-of-time compilation plus PGO (`default.pgo`) |
| JITWatch / escape analysis | `go build -gcflags=-m` |
