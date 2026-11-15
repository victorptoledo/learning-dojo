---
from: javascript
status: draft
concept: "Iterators and generator functions"
from_way: "The iterator protocol with Symbol.iterator and next() returning { value, done }, function* with yield and yield*, and arrays built eagerly by map and filter."
native_way: "__iter__ and __next__ with StopIteration, plain def with yield and yield from, generator expressions, and itertools for lazy slicing and grouping."
sensei_tip: "Python generators are JavaScript generators with lighter syntax: no star, and yield from instead of yield*. The big win is generator expressions, which JavaScript lacks."
---

| JavaScript | Python |
| --- | --- |
| `function* gen() { yield 1 }` | `def gen(): yield 1` |
| `yield* other()` | `yield from other()` |
| `it.next()` returning `{ value, done }` | `next(it)`, `StopIteration` at the end |
| `[Symbol.iterator]()` | `__iter__()` |
| `xs.filter(p).map(f)` (eager arrays) | `(f(x) for x in xs if p(x))` (lazy) |
| `Iterator.prototype.take(5)` / a manual loop | `itertools.islice(it, 5)` |
| `for await (const line of rl)` | `for line in file:` (sync) / `async for` (async) |
