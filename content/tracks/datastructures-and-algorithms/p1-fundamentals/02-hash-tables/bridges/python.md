---
from: python
status: draft
concept: "Hash tables"
from_way: "Using dict and set as magic boxes, and defining __eq__ on a class without thinking about __hash__."
native_way: "dict and set are open-addressing hash tables with a randomised string hash per process. A class that defines __eq__ gets __hash__ = None (unhashable) unless it defines __hash__ too, which enforces the same contract Java only documents."
sensei_tip: "Frozen dataclasses (`@dataclass(frozen=True)`) are Python's records: immutable, with matching __eq__ and __hash__. Since Python 3.7 a dict keeps insertion order, like Java's LinkedHashMap rather than HashMap."
---

| Python | Java | Inside |
| --- | --- | --- |
| `dict` | `LinkedHashMap` | open addressing, insertion ordered |
| `set` | `HashSet` | open addressing |
| `__hash__` / `__eq__` | `hashCode` / `equals` | same contract |
| `@dataclass(frozen=True)` | `record` | immutable, hashable |
| `collections.Counter` | `HashMap<K, Integer>` with `merge` | counting |
| `collections.defaultdict(list)` | `computeIfAbsent(k, x -> new ArrayList<>())` | grouping |
| `PYTHONHASHSEED` | (no per-process seed for String) | hash flooding defence |
