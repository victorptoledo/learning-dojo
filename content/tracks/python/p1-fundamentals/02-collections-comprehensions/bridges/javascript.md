---
from: javascript
status: draft
concept: "Arrays, objects, Map and Set"
from_way: "Arrays with map, filter and reduce, plain objects or Map for lookup, Set for membership, and Object.groupBy for grouping."
native_way: "list, dict and set with comprehensions, Counter and defaultdict for counting and grouping, and tuples as compound keys."
sensei_tip: "A Python dict accepts any hashable key, so it covers both plain objects and Map. A tuple works as a compound key, which a JavaScript Map cannot do by value."
---

| JavaScript | Python |
| --- | --- |
| `xs.filter(p).map(f)` | `[f(x) for x in xs if p(x)]` |
| `xs.reduce((a, x) => a + x, 0)` | `sum(xs)` |
| `new Set(xs).has(x)` | `x in set(xs)` |
| `Object.groupBy(xs, f)` | `defaultdict(list)` and a loop |
| `map.get(k) ?? 0` | `d.get(k, 0)` |
| `{...a, ...b}` | `a \| b` |
| `const [first, ...rest] = xs` | `first, *rest = xs` |
| `xs.slice(1, 3)` / `xs.at(-1)` | `xs[1:3]` / `xs[-1]` |
