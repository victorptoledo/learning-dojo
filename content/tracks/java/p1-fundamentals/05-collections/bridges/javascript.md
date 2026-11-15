---
from: javascript
status: draft
concept: "The standard collections"
from_way: "Array, Map, Set and objects as dictionaries."
native_way: "Typed collections with clear implementations: ArrayList, HashMap, LinkedHashMap, TreeMap."
sensei_tip: "JavaScript's Map keeps insertion order; in Java that is LinkedHashMap, while HashMap has no order and TreeMap sorts."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| [] | new ArrayList<>() |
| new Map() | new LinkedHashMap<>() (ordered) or HashMap |
| new Set() | new HashSet<>() |
| arr.at(-1) | list.getLast() |
| Object.freeze([...]) | List.copyOf(...) |
