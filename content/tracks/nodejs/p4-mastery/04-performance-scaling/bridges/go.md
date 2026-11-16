---
from: go
status: draft
concept: "pprof habits in Node.js"
from_way: "pprof CPU and heap profiles, GOMEMLIMIT and GOGC for the GC, testing.B benchmarks, and one process using all cores with goroutines."
native_way: "--cpu-prof and heap snapshots, --max-old-space-size for the heap, microbenchmarks with care, and several processes for several cores."
sensei_tip: "GOMEMLIMIT and --max-old-space-size play the same role: keep the GC ahead of the container's limit. Go scales up inside one process; Node.js scales out to more processes."
---

| Go | Node.js |
| --- | --- |
| `go tool pprof cpu.out` | `--cpu-prof` + DevTools / speedscope |
| heap profile | Heap snapshot |
| `GOMEMLIMIT` | `--max-old-space-size` |
| `testing.B` | mitata |
| Goroutines on all cores | Replicas / `cluster` |
| `sync.Pool` | Reuse buffers, avoid per-request allocation |
