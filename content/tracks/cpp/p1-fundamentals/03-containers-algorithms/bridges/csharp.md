---
from: csharp
status: draft
concept: "Collections and LINQ"
from_way: "List<T>, Dictionary<K,V>, SortedDictionary and LINQ; enumerators throw if the collection changes."
native_way: "std::vector, std::unordered_map, std::map and the algorithms and ranges libraries; changing a container can silently invalidate iterators."
sensei_tip: "LINQ maps to algorithms and views; foreach over a modified List throws in C#, but in C++ it is silent undefined behaviour."
---

| C# | C++ |
| --- | --- |
| `List<T>` | `std::vector<T>` |
| `Dictionary<K,V>` | `std::unordered_map<K,V>` |
| `SortedDictionary<K,V>` | `std::map<K,V>` |
| `TryGetValue` | `find` / `contains` |
| `list.RemoveAll(p)` | `std::erase_if(v, p)` |
| `OrderBy(x => x.Age)` | `std::ranges::sort(v, {}, &T::age)` |
| `Span<T>` | `std::span<T>` |
