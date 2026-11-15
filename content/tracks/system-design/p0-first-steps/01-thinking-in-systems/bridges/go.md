---
from: go
status: draft
concept: "From one application to a system"
from_way: "A single binary with goroutines, where channels make concurrency feel free."
native_way: "Many binaries on many hosts; concurrency is bounded by downstream capacity, and every hop adds latency and a way to fail."
sensei_tip: "Goroutines are cheap, but the database behind them is not. Little's law applies to the pool you call: 10,000 goroutines waiting on a 50-connection pool only build a queue."
---

| Go habit | System design view |
| --- | --- |
| `http.Get(url)` (no timeout) | always a client with `Timeout` |
| `go func()` everywhere | bounded concurrency, back-pressure |
| `testing.B` benchmarks | latency percentiles under load |
| one binary | replicas behind a load balancer |
| `sync.Map` | a shared, partitioned cache tier |
