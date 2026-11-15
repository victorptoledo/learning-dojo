---
from: python
status: draft
concept: "Iterators and closures"
from_way: "List comprehensions, generators, map, filter and lambda."
native_way: "Iterator adapters chained lazily and closures that capture by reference or by move."
sensei_tip: "Rust iterators are lazy like Python generators: nothing runs until collect, sum or a for loop consumes them. iter() borrows, into_iter() consumes."
---

| Python | Rust |
| --- | --- |
| [x * 2 for x in xs if x > 0] | xs.iter().filter(\|x\| **x > 0).map(\|x\| x * 2).collect() |
| sum(xs) | xs.iter().sum() |
| lambda x: x + 1 | \|x\| x + 1 |
| enumerate(xs) | xs.iter().enumerate() |
| zip(a, b) | a.iter().zip(b) |
| yield | impl Iterator |
