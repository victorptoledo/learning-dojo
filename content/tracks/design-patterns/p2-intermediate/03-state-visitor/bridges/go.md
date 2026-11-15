---
from: go
status: draft
concept: "State, visitor, composite and template method"
from_way: "String or int states checked in every function, or visitor interfaces translated from Java."
native_way: "Interfaces with an unexported marker method to close the set, type switches for visitor-style operations, and structs holding slices of children for composites."
sensei_tip: "Go's type switch is not exhaustive. A linter such as `exhaustive` (for enums) or `go-sumtype` (for interface sum types) can add the compile-time check."
---

| Go | Pattern |
| --- | --- |
| state structs behind an interface | state |
| `switch v := n.(type) { case *Circle: ... }` | visitor |
| `go/ast.Inspect`, `ast.Walk` | classic visitor |
| a struct with `Children []Node` | composite |
| `iter.Seq[T]` and range-over-func | iterator |
