---
from: go
status: draft
concept: "Processes and threads in Go"
from_way: "Mapping goroutines one to one to OS threads."
native_way: "Goroutines are scheduled by the Go runtime (the GMP scheduler) on GOMAXPROCS OS threads, with work stealing and preemption; os/exec starts processes."
sensei_tip: "Start a goroutine per request freely; bound CPU-heavy work with a worker pool, and remember that GOMAXPROCS follows the container's CPU limit since Go 1.25."
---

| In Go | Underneath |
| --- | --- |
| `go f()` | a goroutine (a few KiB) |
| `GOMAXPROCS` | OS threads running Go code |
| `runtime.Gosched()` | yield the processor |
| `exec.Command` | fork/exec of a child |
| `runtime/trace` | a timeline of the scheduler |
