---
from: python
status: draft
concept: "Lazy by default"
from_way: "Building lists with append before looping, file.readlines(), Python 2 habits such as xrange, izip and iteritems, for loops that re-yield another generator item by item, and hand-written chunking."
native_way: "Generators and generator expressions end to end, iterating files directly, range, zip and dict.items being lazy views or iterators, yield from, and itertools.batched and pairwise."
sensei_tip: "Look for functions that build a list only to return it for one loop; turning append into yield usually halves their length and their memory."
---

| Older habit | Python 3.15 |
| --- | --- |
| `result = []` ... `result.append(x)` ... `return result` | `yield x` |
| `for line in f.readlines():` | `for line in f:` |
| `xrange`, `itertools.izip`, `d.iteritems()` | `range`, `zip`, `d.items()` |
| `for x in inner(): yield x` | `yield from inner()` |
| `[xs[i:i + n] for i in range(0, len(xs), n)]` | `itertools.batched(xs, n)` |
| `zip(xs, xs[1:])` | `itertools.pairwise(xs)` |
| `gen.next()` | `next(gen)` |
