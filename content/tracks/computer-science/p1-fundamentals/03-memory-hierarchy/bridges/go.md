---
from: go
status: draft
concept: "Cache-friendly Go"
from_way: "Assuming slices of structs and slices of pointers behave alike."
native_way: "[]T stores structs contiguously; []*T stores pointers to scattered values; field order affects struct size through alignment padding."
sensei_tip: "Prefer []T over []*T in hot paths, order struct fields from large to small to reduce padding, and pad per-goroutine counters to 64 bytes."
---

| In Go | Underneath |
| --- | --- |
| `[]Point` | contiguous structs |
| `[]*Point` | pointer chasing |
| `unsafe.Sizeof(s)` | size including padding |
| `[2][1024]int` | one contiguous block |
| `sync/atomic` counters in one struct | candidates for false sharing |
