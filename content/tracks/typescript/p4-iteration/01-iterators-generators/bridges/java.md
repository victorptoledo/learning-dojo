---
from: java
status: draft
concept: "Iterable, Iterator and lazy Streams"
from_way: "Iterable<T> and Iterator<T> with hasNext() and next(), lazy Stream pipelines that are single-use, and hand-written iterator classes for custom sequences."
native_way: "Iterable<T> with [Symbol.iterator](), iterators whose next() returns { done, value }, generator functions that write the iterator for you, and lazy iterator helpers."
sensei_tip: "A generator is the iterator class you never have to write: local variables are the state and yield is the hasNext/next pair. Like a Stream, a generator object is single-use."
---

| Java | TypeScript |
| --- | --- |
| `Iterable<T>` | `Iterable<T>` |
| `it.hasNext()` / `it.next()` | `const r = it.next(); r.done / r.value` |
| An `Iterator` class with fields | `function*` with local variables and `yield` |
| `stream.filter(...).map(...).limit(5).toList()` | `Iterator.from(xs).filter(...).map(...).take(5).toArray()` |
| `Stream.iterate(0, n -> n + 1)` | `function* naturals() { for (let n = 0; ; n++) yield n; }` |
| A Stream is consumed once | A generator object is consumed once |
| `try (var s = stream)` / `onClose` | `finally` in the generator, run by `return()` |
