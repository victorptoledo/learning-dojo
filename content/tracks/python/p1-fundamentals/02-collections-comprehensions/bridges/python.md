---
from: python
status: draft
concept: "Modern collection idioms"
from_way: "map and filter with lambdas, dict(a, **b) or update() to merge, OrderedDict to keep order, has_key or try/except KeyError for counting, and cmp functions for sorting."
native_way: "Comprehensions, the | and |= dict operators, plain dicts that keep insertion order, Counter and defaultdict, and key functions with tuple keys."
sensei_tip: "OrderedDict is only needed for move_to_end and order-sensitive equality; a plain dict has kept insertion order since 3.7."
---

| Older habit | Python 3.15 |
| --- | --- |
| `list(map(lambda x: x * 2, filter(lambda x: x > 0, xs)))` | `[x * 2 for x in xs if x > 0]` |
| `merged = dict(a, **b)` / `c = a.copy(); c.update(b)` | `merged = a \| b` |
| `OrderedDict()` to keep order | `{}` |
| `if k in d: d[k] += 1 else: d[k] = 1` | `Counter(...)` or `d[k] = d.get(k, 0) + 1` |
| `sorted(xs, cmp=...)` (Python 2) | `sorted(xs, key=...)` (`functools.cmp_to_key` if you must) |
| `zip(a, b)` that silently drops the tail | `zip(a, b, strict=True)` |
