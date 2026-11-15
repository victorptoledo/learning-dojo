---
from: javascript
status: draft
concept: "Standard collections"
from_way: "Array, Map, Set and plain objects used as dictionaries."
native_way: "Vec, HashMap, HashSet and BTreeMap, all typed."
sensei_tip: "Map preserves insertion order in JavaScript; HashMap does not. Use BTreeMap when you need a predictable order, for example in tests and reports."
---

| JavaScript / TypeScript | Rust |
| --- | --- |
| Array | Vec<T> |
| Map | HashMap<K, V> |
| Set | HashSet<T> |
| object as dictionary | HashMap<String, T> |
| arr.sort() | v.sort() |
| string and template literals | String and format! |
