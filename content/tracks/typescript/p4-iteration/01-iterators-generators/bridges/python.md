---
from: python
status: draft
concept: "Iterators, generators and itertools"
from_way: "__iter__ and __next__ with StopIteration, generator functions with yield and yield from, itertools for lazy pipelines, close() running finally."
native_way: "[Symbol.iterator]() and next() returning { done, value }, function* with yield and yield*, ES2025 iterator helpers, return() running finally."
sensei_tip: "This is Python's model almost one to one. The differences: the end is { done: true } instead of an exception, and the generator's type has three parameters, Generator<Yield, Return, Next>, like Python's Generator[Y, S, R] in a different order."
---

| Python | TypeScript |
| --- | --- |
| `def __iter__(self)` | `*[Symbol.iterator]()` |
| `raise StopIteration` | `{ done: true }` (a generator's `return`) |
| `yield from other` | `yield* other` |
| `itertools.islice(it, 5)` | `Iterator.from(it).take(5)` |
| `map(f, it)` / `filter(p, it)` (lazy) | `Iterator.from(it).map(f)` / `.filter(p)` |
| `list(it)` | `[...it]` or `it.toArray()` |
| `zip(a, b)` | Write a `zip` generator (no built-in yet) |
| `Generator[int, None, str]` | `Generator<number, string, undefined>` |
