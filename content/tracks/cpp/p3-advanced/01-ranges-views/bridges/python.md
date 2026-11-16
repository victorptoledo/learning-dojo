---
from: python
status: draft
concept: "Ranges and generators"
from_way: "Generators with yield, itertools, and comprehensions that build lists eagerly."
native_way: "std::generator with co_yield, view adaptors that are lazy like itertools, and ranges::to to build containers."
sensei_tip: "A Python generator function becomes a std::generator coroutine almost line by line: `yield x` is `co_yield x;`."
---

| Python | C++ |
| --- | --- |
| `def lines(text): ... yield line` | `std::generator<std::string_view> lines(...) { co_yield line; }` |
| `(f(x) for x in xs if p(x))` | `xs \| views::filter(p) \| views::transform(f)` |
| `list(gen)` | `gen \| std::ranges::to<std::vector>()` |
| `enumerate`, `zip` | `std::views::enumerate`, `std::views::zip` |
| `itertools.islice(it, n)` | `std::views::take(n)` |
| `sorted(xs, key=f)` | `std::ranges::sort(xs, {}, f)` |
