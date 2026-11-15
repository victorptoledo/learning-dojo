---
from: python
status: draft
concept: "Standard collections"
from_way: "list, dict, set and collections such as deque and defaultdict."
native_way: "Vec, HashMap, HashSet, BTreeMap and VecDeque, with the entry API."
sensei_tip: "d[key] = d.get(key, 0) + 1 becomes *map.entry(key).or_insert(0) += 1. HashMap has no order; BTreeMap keeps keys sorted, like a dict that is always sorted."
---

| Python | Rust |
| --- | --- |
| list | Vec<T> |
| dict | HashMap<K, V> |
| sorted(d.items()) | BTreeMap<K, V> |
| set | HashSet<T> |
| collections.deque | VecDeque<T> |
| defaultdict(int) | entry(k).or_insert(0) |
