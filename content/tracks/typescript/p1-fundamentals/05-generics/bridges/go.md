---
from: go
status: draft
concept: "Type parameters and constraints"
from_way: "func Map[T, U any](xs []T, f func(T) U) []U with constraints as interfaces (comparable, ~int | ~string); no methods with type parameters."
native_way: "Type parameters on functions, types, classes and methods, constraints as any type (extends), plus keyof and indexed access."
sensei_tip: "Go's constraint `~int | ~string` is a union in TypeScript: `T extends number | string`. Methods can be generic too, which Go does not allow."
---

| Go | TypeScript |
| --- | --- |
| `func First[T any](xs []T) (T, bool)` | `function first<T>(xs: T[]): T \| undefined` |
| `[K comparable, V any]` | `<K, V>` (any value can be a Map key) |
| `~int \| ~string` constraint | `T extends number \| string` |
| No generic methods | `class Box<T> { map<U>(f: (v: T) => U) }` |
| Struct field access by name only | `T[K]` with `K extends keyof T` |
