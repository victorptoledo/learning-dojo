---
from: java
status: draft
concept: "Hash tables"
from_way: "Using a HashMap without knowing how hashing and equality interact, with mutable keys and hand-written hashCode methods."
native_way: "A hash function maps the key to a bucket; collisions are handled by chaining (HashMap) or open addressing (IdentityHashMap). Keys should be immutable records whose equals and hashCode use the same fields."
sensei_tip: "If you know the final size, create the map with `HashMap.newHashMap(expected)` (Java 19+): it computes the capacity for the 0.75 load factor so no rehash ever happens. And a `record` key gives you a correct equals/hashCode pair with zero code."
---

| Java | Inside |
| --- | --- |
| `HashMap` | chaining, power-of-two buckets, `h ^ (h >>> 16)`, resize at 0.75, treeified bins |
| `HashSet` | a `HashMap` whose values are a dummy object |
| `LinkedHashMap` | `HashMap` plus a doubly linked list through the entries |
| `ConcurrentHashMap` | per-bin locking on writes, lock-free reads |
| `IdentityHashMap` | open addressing, `==` and `System.identityHashCode` |
| `EnumMap` | an array indexed by `ordinal()` |
| `record Key(long user, String device)` | correct `equals` and `hashCode` for free |
