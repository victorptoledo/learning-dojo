---
from: go
status: draft
concept: "The standard collections"
from_way: "Slices and maps built into the language, and the slices and maps packages."
native_way: "Interfaces and implementations from java.util, chosen by need."
sensei_tip: "Go's map iteration is randomised on purpose; Java's HashMap order is unspecified too. Use TreeMap or LinkedHashMap when order matters."
---

| Go | Java 25 |
| --- | --- |
| []int{1, 2} | List.of(1, 2) |
| map[string]int{} | new HashMap<String, Integer>() |
| slices.Sort(xs) | Collections.sort(xs) or list.sort(null) |
| m[k]++ | map.merge(k, 1, Integer::sum) |
| v, ok := m[k] | map.containsKey(k) / get |
