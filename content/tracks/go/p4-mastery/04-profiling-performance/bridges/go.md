---
from: go
status: draft
concept: "Performance habits that changed"
from_way: "Ballast allocations to calm the GC, GOGC tuned per service, b.N loops with ResetTimer, comparing single benchmark runs, the pprof graph view, diffing goroutine dumps for leaks, and hand inlining hot functions."
native_way: "GOMEMLIMIT instead of ballast (1.19), PGO (1.21), b.Loop (1.24), the flight recorder (1.25), Green Tea GC and the flame graph as pprof's default view (1.26), and the goroutineleak profile (1.27)."
sensei_tip: "If a service still allocates a large []byte ballast at start-up, replace it with GOMEMLIMIT: it does what the ballast tried to do, without the wasted memory."
---

| Older Go | Go 1.27 |
| --- | --- |
| `ballast := make([]byte, 10<<30)` | `GOMEMLIMIT=...` (1.19) |
| hand inlining hot calls | `default.pgo` (1.21) |
| `for i := 0; i < b.N; i++` | `for b.Loop()` (1.24) |
| "reproduce it with tracing on" | `trace.FlightRecorder` (1.25) |
| `GOEXPERIMENT=greenteagc` | Green Tea by default (1.26) |
| pprof graph view first | flame graph first (1.26) |
| diffing goroutine dumps | `/debug/pprof/goroutineleak` (1.27) |
