---
from: java
status: draft
concept: "Values, arrays, lists and maps"
from_way: "Primitives and boxed types, null as the default for objects, ArrayList and HashMap from the collections framework, and implicit widening from int to long."
native_way: "Every type has a usable zero value, slices and maps are built into the language, conversions are always explicit, and strings are bytes with runes for characters."
sensei_tip: "A slice is not an ArrayList: a sub-slice shares memory with the original. When you hand one out, think about who may append to it."
---

| Java | Go |
| --- | --- |
| `int x = 0;` / `Integer x = null;` | `var x int` (always 0, never null) |
| `long y = x;` | `y := int64(x)` |
| `List<String> names = new ArrayList<>();` | `var names []string` |
| `names.add("a")` | `names = append(names, "a")` |
| `list.subList(1, 3)` | `s[1:3]` (shares the array, like subList's view) |
| `Map<String, Integer> m = new HashMap<>();` | `m := map[string]int{}` |
| `m.getOrDefault(k, 0)` | `m[k]` (zero value when missing) |
| `m.containsKey(k)` | `_, ok := m[k]` |
| `char` (UTF-16 unit) | `rune` (a full code point) |
