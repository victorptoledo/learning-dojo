---
from: java
status: draft
concept: "Collections without the boilerplate"
from_way: "`new ArrayList<>(Arrays.asList(...))`, `Collections.unmodifiableList`, get-then-put loops to count, and `list.get(list.size() - 1)`."
native_way: "`List.of`, `Map.of` and `copyOf` for fixed data, `merge` and `computeIfAbsent` to count and group, and `getLast()` on any sequenced collection."
sensei_tip: "Every `if (map.get(k) == null) map.put(k, ...)` in your code is a `computeIfAbsent` or `merge` waiting to happen."
---

| Java 8 | Java 21+ |
| --- | --- |
| `Collections.unmodifiableList(Arrays.asList(a, b))` | `List.of(a, b)` |
| `if (!m.containsKey(k)) m.put(k, new ArrayList<>()); m.get(k).add(v);` | `m.computeIfAbsent(k, x -> new ArrayList<>()).add(v);` |
| `m.put(k, m.getOrDefault(k, 0) + 1);` | `m.merge(k, 1, Integer::sum);` |
| `list.get(list.size() - 1)` | `list.getLast()` |
| Iterator with `it.remove()` | `list.removeIf(...)` |
| `Stack<T>` | `ArrayDeque<T>` |
