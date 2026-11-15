---
from: javascript
status: draft
concept: "Node profiling and V8 memory"
from_way: "node --prof or --cpu-prof, Chrome DevTools flame charts and heap snapshots, clinic.js, --max-old-space-size for memory, and benchmark.js."
native_way: "pprof CPU and heap profiles with a flame graph UI, goroutine profiles for stuck work, the execution tracer for latency, and GOMEMLIMIT as the memory knob."
sensei_tip: "A Node event loop blocked by CPU work shows up as latency for everyone; in Go other goroutines keep running, so look for lock contention and allocation pressure instead."
---

| Node | Go |
| --- | --- |
| `node --cpu-prof` | `go test -cpuprofile` / `/debug/pprof/profile` |
| DevTools heap snapshot | heap profile |
| clinic.js doctor | `go tool trace` |
| `--max-old-space-size` | `GOMEMLIMIT` |
| benchmark.js | `testing.B` and benchstat |
| event loop lag metric | scheduler metrics in `runtime/metrics` |
