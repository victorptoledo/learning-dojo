---
from: python
status: draft
concept: "The standard collections"
from_way: "list, dict, set and collections helpers, all mutable and untyped."
native_way: "List, Map and Set interfaces with typed implementations, and unmodifiable factories List.of and Map.of."
sensei_tip: "d.get(k, 0) + 1 becomes map.merge(k, 1, Integer::sum). List.of returns an immutable list: adding to it throws, unlike a Python list."
---

| Python | Java 25 |
| --- | --- |
| [1, 2, 3] | List.of(1, 2, 3) |
| dict() | new HashMap<>() |
| sorted dict | TreeMap |
| defaultdict(list) | computeIfAbsent(k, _ -> new ArrayList<>()) |
| Counter | merge(k, 1, Integer::sum) |
| xs[-1] | list.getLast() |
