---
from: java
status: draft
concept: "Streams and lambdas"
from_way: "`list.stream().filter(...).map(...).collect(Collectors.toList())`; lambdas capture only effectively final variables."
native_way: "`v.iter().filter(...).map(...).collect::<Vec<_>>()`; closures can borrow, mutate (`FnMut`) or own (`move`) what they capture."
sensei_tip: "No boxing and no virtual calls: an iterator chain compiles to the same loop you would write by hand."
---

| Java | Rust |
| --- | --- |
| `stream()` | `iter()` / `into_iter()` |
| `reduce(0, Integer::sum)` | `fold(0, \|a, b\| a + b)` or `sum()` |
| `anyMatch` / `allMatch` | `any` / `all` |
| `findFirst()` | `next()` or `find(...)` |
| `IntStream.range(0, n)` | `0..n` |
| `Collectors.groupingBy` | a `HashMap` and `entry(...).or_default()` |
