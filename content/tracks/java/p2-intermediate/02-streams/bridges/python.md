---
from: python
status: draft
concept: "Pipelines over collections"
from_way: "List comprehensions, generators, itertools and collections.Counter."
native_way: "Streams: lazy pipelines of filter, map and collectors such as groupingBy."
sensei_tip: "sum(x.amount for x in sales if x.region == 'North') is sales.stream().filter(...).mapToLong(Sale::amount).sum(). groupingBy plays the role of itertools.groupby without sorting first."
---

| Python | Java 25 |
| --- | --- |
| [f(x) for x in xs if p(x)] | xs.stream().filter(p).map(f).toList() |
| sum(...) | mapToInt(...).sum() |
| Counter(words) | groupingBy(w -> w, counting()) |
| any(...) | anyMatch(...) |
| sorted(xs, key=...) | sorted(Comparator.comparing(...)) |
