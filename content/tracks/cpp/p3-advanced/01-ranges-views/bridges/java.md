---
from: java
status: draft
concept: "Ranges and views"
from_way: "The Streams API: lazy intermediate operations, a terminal collect, and streams that can be consumed once."
native_way: "Views are lazy like streams and composed with |, std::ranges::to collects them, and std::generator writes lazy sequences."
sensei_tip: "`stream().filter(p).map(f).toList()` reads `v | views::filter(p) | views::transform(f) | ranges::to<std::vector>()`."
---

| Java | C++ |
| --- | --- |
| `list.stream()` | the container itself (a range) |
| `.filter(p).map(f)` | `\| std::views::filter(p) \| std::views::transform(f)` |
| `.collect(Collectors.toList())` | `\| std::ranges::to<std::vector>()` |
| `.limit(n)` / `.skip(n)` | `std::views::take(n)` / `std::views::drop(n)` |
| `Comparator.comparing(Person::age)` | a projection: `std::ranges::sort(v, {}, &Person::age)` |
| `IntStream.range(0, n)` | `std::views::iota(0, n)` |
| a stream is used once | a generator is iterated once; most views can be re-iterated |
