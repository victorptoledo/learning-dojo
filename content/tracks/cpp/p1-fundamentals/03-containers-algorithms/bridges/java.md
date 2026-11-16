---
from: java
status: draft
concept: "Collections and streams"
from_way: "ArrayList, HashMap, TreeMap and the Streams API; iterators throw ConcurrentModificationException when the collection changes."
native_way: "std::vector, std::unordered_map, std::map and the algorithms library; iterators are invalidated silently, which is undefined behaviour to use."
sensei_tip: "C++ will not throw when you modify a vector while iterating: build with sanitizers and use std::erase_if."
---

| Java | C++ |
| --- | --- |
| `ArrayList<Integer>` | `std::vector<int>` (no boxing) |
| `HashMap<K, V>` | `std::unordered_map<K, V>` |
| `TreeMap<K, V>` | `std::map<K, V>` |
| `map.get(k)` returns null | `find` returns `end()`; `operator[]` inserts! |
| `list.removeIf(...)` | `std::erase_if(v, ...)` |
| `Collections.sort(list, cmp)` | `std::ranges::sort(v, cmp)` |
| ConcurrentModificationException | silent invalidation (UB) |
