---
from: go
status: draft
concept: "Iterators and closures"
from_way: "for range loops, with the iter package and range-over-func since Go 1.23."
native_way: "Iterator chains with map, filter and fold, compiled into loops."
sensei_tip: "Go encourages plain loops; Rust offers both. A chain is usually as fast as the loop, so pick what reads better. Closures that outlive the function need move, like capturing by value."
---

| Go | Rust |
| --- | --- |
| for i, x := range xs | for (i, x) in xs.iter().enumerate() |
| manual filter loop | .filter(...) |
| func(x int) int { return x + 1 } | \|x\| x + 1 |
| iter.Seq[T] | impl Iterator<Item = T> |
| slices.ContainsFunc | .any(...) |
