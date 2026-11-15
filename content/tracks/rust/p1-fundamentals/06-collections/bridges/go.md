---
from: go
status: draft
concept: "Standard collections"
from_way: "Slices, maps and strings, with sort and maps packages for the rest."
native_way: "Vec, HashMap, BTreeMap, HashSet and String versus &str."
sensei_tip: "A Go string is a read-only slice of bytes, close to Rust's &str. String is the owned, growable version, like a []byte you own."
---

| Go | Rust |
| --- | --- |
| []T | Vec<T> (owned) or &[T] (borrowed) |
| map[K]V | HashMap<K, V> |
| map[K]struct{} | HashSet<K> |
| maps.Keys + slices.Sort | BTreeMap<K, V> |
| string | &str or String |
| strings.Builder | String with push_str |
