---
from: go
status: draft
concept: "Observing every layer from Go"
from_way: "Optimising before profiling."
native_way: "net/http/pprof exposes CPU, heap, goroutine, block and mutex profiles; runtime/trace shows the scheduler, GC and syscalls on a timeline; GODEBUG=gctrace=1 prints GC pauses."
sensei_tip: "Import `_ \"net/http/pprof\"`, then `go tool pprof -http=: host/debug/pprof/profile`: one command from a slow service to a flame graph."
---

| In Go | Underneath |
| --- | --- |
| `/debug/pprof/profile` | CPU profile |
| `/debug/pprof/goroutine` | where every goroutine waits |
| `runtime/trace` | scheduler and GC timeline |
| `GODEBUG=gctrace=1` | GC pauses |
| block and mutex profiles | time lost waiting |
