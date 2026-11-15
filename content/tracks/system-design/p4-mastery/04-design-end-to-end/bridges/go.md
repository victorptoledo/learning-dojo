---
from: go
status: draft
concept: "From components to a whole system"
from_way: "Writing fast, simple Go services with goroutines and channels."
native_way: "Composing many such services into a system with clear data ownership, async streams, bounded concurrency and contained failures."
sensei_tip: "Go is a common choice for connection gateways and stream processors because goroutines make 100,000 connections per host practical; the design work is in what happens when that host dies."
---

| Go habit | End-to-end design habit |
| --- | --- |
| a goroutine per connection | gateways sized by connections per host |
| channels between stages | a log between services |
| `context` deadlines | latency budgets per hop |
| `pprof` | capacity planning from measured costs |
| a design doc in the repo | ADRs and a review checklist |
