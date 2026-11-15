---
from: go
status: draft
concept: "sync and atomics since Go 1.19"
from_way: "atomic.AddInt64(&n, 1) on plain int64 fields that had to be 64-bit aligned, atomic.Value with type assertions, sync.Once plus package variables for lazy values, and wg.Add and Done by hand."
native_way: "Typed atomics (atomic.Int64, atomic.Bool, atomic.Pointer[T]) since 1.19, sync.OnceFunc, OnceValue and OnceValues since 1.21, and wg.Go since 1.25."
sensei_tip: "Typed atomics cannot be read without Load by mistake, and they are always aligned. go fix's atomictypes modernizer (1.27) converts the old function calls for you."
---

| Older Go | Go 1.27 |
| --- | --- |
| `var n int64; atomic.AddInt64(&n, 1)` | `var n atomic.Int64; n.Add(1)` |
| `atomic.Value` + `v.Load().(*Config)` | `atomic.Pointer[Config]` |
| `var once sync.Once; var cfg *Config` + a getter | `var getConfig = sync.OnceValue(load)` |
| `wg.Add(1); go func() { defer wg.Done() ... }()` | `wg.Go(func() { ... })` |
| manual conversions | `go fix` modernizers `atomictypes` and `waitgroupgo` |
