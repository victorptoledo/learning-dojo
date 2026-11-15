---
from: csharp
status: draft
concept: "Hash tables"
from_way: "Overriding Equals without GetHashCode, or using mutable classes as Dictionary keys."
native_way: "Dictionary<K,V> and HashSet<T> are hash tables with chaining into an entries array. The key's GetHashCode and Equals follow the same contract as Java's, and an IEqualityComparer<T> can supply them from outside."
sensei_tip: "A `record` or `record struct` key gets value-based Equals and GetHashCode automatically, and `HashCode.Combine(a, b)` builds a good hash by hand. `new Dictionary<K,V>(capacity)` pre-sizes like Java's `HashMap.newHashMap`."
---

| C# | Java | Note |
| --- | --- | --- |
| `Dictionary<K,V>` | `HashMap<K,V>` | chaining |
| `HashSet<T>` | `HashSet<T>` | |
| `GetHashCode` / `Equals` | `hashCode` / `equals` | same contract |
| `IEqualityComparer<T>` | (a wrapper key, or a custom map) | external equality |
| `record` | `record` | value equality for free |
| `HashCode.Combine(a, b)` | `Objects.hash(a, b)` | combine fields |
| `ConcurrentDictionary<K,V>` | `ConcurrentHashMap<K,V>` | thread safe |
