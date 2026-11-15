---
from: go
status: draft
concept: "Hash tables"
from_way: "Using map[K]V without knowing that iteration order is randomised on purpose, or trying to use a slice as a key."
native_way: "Go maps are built into the language; keys must be comparable types (no slices, maps or functions), and equality is structural for structs and arrays. Since Go 1.24 maps use a Swiss-table design (open addressing with metadata groups), with a per-map random seed."
sensei_tip: "A struct of comparable fields is a perfect composite key: `map[struct{user int64; device string}]Session`. Iteration order is deliberately random, so sort the keys (`slices.Sorted(maps.Keys(m))`) when you need a stable order."
---

| Go | Java | Note |
| --- | --- | --- |
| `map[K]V` | `HashMap<K,V>` | built in, open addressing (Go 1.24+) |
| `map[K]struct{}` | `HashSet<K>` | a set |
| struct key | `record` key | structural equality |
| `v, ok := m[k]` | `map.get(k)` / `containsKey` | presence check |
| `make(map[K]V, n)` | `HashMap.newHashMap(n)` | pre-size |
| `sync.Map` | `ConcurrentHashMap` | concurrent |
| random iteration order | `HashMap` order | never rely on it |
