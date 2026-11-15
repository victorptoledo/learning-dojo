---
from: csharp
status: draft
concept: "Standard collections"
from_way: "List<T>, Dictionary<K,V>, HashSet<T>, SortedDictionary and Queue."
native_way: "Vec<T>, HashMap<K,V>, HashSet<T>, BTreeMap and VecDeque."
sensei_tip: "The names change but the ideas match. TryGetValue becomes map.get(&k), which returns an Option, and CollectionsMarshal tricks become the entry API."
---

| C# | Rust |
| --- | --- |
| List<T> | Vec<T> |
| Dictionary<K,V> | HashMap<K,V> |
| SortedDictionary<K,V> | BTreeMap<K,V> |
| HashSet<T> | HashSet<T> |
| Queue<T> | VecDeque<T> |
| string / StringBuilder | String / &str |
