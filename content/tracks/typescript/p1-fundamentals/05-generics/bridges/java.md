---
from: java
status: draft
concept: "Generic types and constraints"
from_way: "Generic classes and methods with bounded type parameters (<T extends Comparable<T>>), wildcards (? extends, ? super), and type erasure at runtime."
native_way: "Generic functions and types with constraints (T extends ...), keyof and indexed access to relate keys and values, inference at every call, and full erasure."
sensei_tip: "TypeScript has no wildcards: variance is inferred from structure. And keyof plus T[K] has no Java equivalent: it lets a type talk about the keys of another type."
---

| Java | TypeScript |
| --- | --- |
| `<T> T first(List<T> items)` | `function first<T>(items: T[]): T \| undefined` |
| `<T extends Comparable<T>>` | `<T extends { compareTo(other: T): number }>` |
| `List<? extends Number>` | `readonly number[]` (variance from structure) |
| `Class<T>` tokens to keep types at runtime | Not possible: pass a function or schema instead |
| No equivalent | `K extends keyof T`, `T[K]` |
