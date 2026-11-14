---
from: java
status: draft
concept: "Standard collections"
from_way: "`ArrayList`, `HashMap` and `StringBuilder`; `map.get(k)` returns `null` when the key is missing; `ConcurrentModificationException` at run time."
native_way: "`Vec<T>`, `HashMap<K, V>` and `String`; `map.get(k)` returns `Option<&V>`; changing a collection while it is borrowed is a compile error."
sensei_tip: "`map.entry(k).or_insert(0)` is `merge(k, 1, Integer::sum)` without the boxing."
---

| Java | Rust |
| --- | --- |
| `new ArrayList<>()` | `Vec::new()` or `vec![]` |
| `list.get(i)` (throws) | `v[i]` (panics) or `v.get(i)` (Option) |
| `map.getOrDefault(k, 0)` | `map.get(k).copied().unwrap_or(0)` |
| `map.merge(k, 1, Integer::sum)` | `*map.entry(k).or_insert(0) += 1` |
| `computeIfAbsent(k, x -> new ArrayList<>()).add(v)` | `map.entry(k).or_default().push(v)` |
| `TreeMap` | `BTreeMap` |
