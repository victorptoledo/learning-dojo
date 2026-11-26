---
from: go
status: draft
concept: "A small sound core vs a large expressive one"
from_way: "Go's type system is small and nearly sound: no variance on generic types (all invariant), type assertions are checked at runtime and panic when wrong."
native_way: "TypeScript's type system is large and unsound by design: generic types can be covariant or contravariant, and type assertions are never checked."
sensei_tip: "In Go, x.(T) panics when it is wrong; in TypeScript, x as T silently lies. Every as in a TypeScript codebase is a place where a Go program would have checked at runtime."
---

| Go | TypeScript |
| --- | --- |
| `v, ok := x.(User)` (checked) | `x as User` (unchecked); use a type predicate instead |
| Generic types are invariant | Variance inferred, or `in` / `out` |
| Fast compiler by design | Expensive types can slow `tsc`; measure them |
| `go build -x`, `-gcflags` | `tsc --extendedDiagnostics` |
