---
from: java
status: draft
concept: "Lazy sequences: streams versus generators"
from_way: "Iterable and Iterator interfaces with hasNext and next, lazy Stream pipelines with filter and map, Stream.iterate for infinite sources, and BufferedReader.lines() for large files."
native_way: "__iter__ and __next__ with StopIteration, generator functions with yield, generator expressions, itertools, and file objects that yield lines lazily."
sensei_tip: "A generator is an Iterator you write as a normal function: no class, no hasNext bookkeeping. Like a Java Stream, it can be consumed only once."
---

| Java | Python |
| --- | --- |
| `implements Iterator<T>` with `hasNext` / `next` | a function with `yield` |
| `NoSuchElementException` | `StopIteration` |
| `list.stream().filter(p).map(f)` | `(f(x) for x in xs if p(x))` |
| `Stream.iterate(0, n -> n + 2).limit(5)` | `islice(count(0, 2), 5)` |
| `Files.lines(path)` | `for line in path.open():` |
| `Collectors.groupingBy` | `groupby` on sorted data, or `defaultdict` |
| a stream reused: `IllegalStateException` | a generator reused: silently empty |
