---
from: java
status: draft
concept: "Streams after Java 8"
from_way: "Java 8 streams with collect(Collectors.toList()) everywhere, manual grouping maps, and no way to write your own intermediate operation."
native_way: "toList(), groupingBy with downstream collectors and map factories, takeWhile and dropWhile (9), and Stream.gather for custom steps (24)."
sensei_tip: "Most `collect(Collectors.toList())` calls in a Java 8 codebase can become `toList()`, as long as nobody adds to the result."
---

| Java 8 | Java 16–24 |
| --- | --- |
| `.collect(Collectors.toList())` | `.toList()` (unmodifiable) |
| Loop with `break` on a condition | `.takeWhile(cond)` |
| `Stream.iterate(seed, f).limit(n)` | `Stream.iterate(seed, hasNext, next)` |
| `Optional.ofNullable(x).map(Stream::of).orElseGet(Stream::empty)` | `Stream.ofNullable(x)` |
| Hand-written batching | `.gather(Gatherers.windowFixed(n))` |
