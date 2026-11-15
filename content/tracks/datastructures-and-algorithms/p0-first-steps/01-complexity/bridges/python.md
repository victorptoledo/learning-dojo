---
from: python
status: draft
concept: "Cost of operations and amortised growth"
from_way: "Writing `if x in my_list` inside a loop, or building a string with `+=`, because Python makes everything look equally cheap."
native_way: "A Python `list` is a dynamic array (over-allocating by about 1/8 when it grows), so `append` is amortised O(1) but `in`, `index` and `insert(0, x)` are O(n). `set` and `dict` are hash tables with O(1) expected lookups."
sensei_tip: "The CPython wiki page 'TimeComplexity' lists the cost of every list, deque, set and dict operation. `x in some_set` instead of `x in some_list` is the most common one-line speed-up in Python code reviews."
---

| Python | Java | Cost |
| --- | --- | --- |
| `lst.append(x)` | `list.add(x)` | amortised O(1) |
| `lst[i]` | `list.get(i)` | O(1) |
| `lst.insert(0, x)` / `lst.pop(0)` | `list.add(0, x)` / `list.remove(0)` | O(n) |
| `x in lst` | `list.contains(x)` | O(n) |
| `x in s` (set) / `d[k]` (dict) | `set.contains(x)` / `map.get(k)` | O(1) expected |
| `sorted(lst)` | `List.sort` | O(n log n), TimSort in both |
| `"".join(parts)` | `String.join` / `StringBuilder` | O(total length) |
