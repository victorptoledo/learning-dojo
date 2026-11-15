---
from: csharp
status: draft
concept: "The standard collections"
from_way: "List<T>, Dictionary<K,V>, HashSet<T>, SortedDictionary and immutable collections."
native_way: "ArrayList, HashMap, HashSet, TreeMap and unmodifiable views and copies."
sensei_tip: "Dictionary.TryGetValue becomes map.get(k) returning null, or getOrDefault. Java's List.of is like ImmutableList.Create, and copyOf takes a snapshot."
---

| C# | Java 25 |
| --- | --- |
| List<T> | List<T> / ArrayList |
| Dictionary<K,V> | Map<K,V> / HashMap |
| SortedDictionary | TreeMap |
| TryGetValue | get or getOrDefault |
| ImmutableList.Create(...) | List.of(...) |
