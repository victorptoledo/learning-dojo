---
from: go
status: draft
concept: "Signatures, variadics and closures"
from_way: "No default or named arguments (functional options or config structs instead), variadic ...T, multiple return values, and closures capturing variables."
native_way: "Defaults and keyword-only parameters replace functional options, *args is the variadic, a tuple return gives several values, and closures work the same way."
sensei_tip: "The Go functional-options pattern exists because Go has no keyword arguments. In Python, def new_server(addr, *, timeout=30, tls=None) is the idiom."
---

| Go | Python |
| --- | --- |
| `NewServer(addr, WithTimeout(30))` | `new_server(addr, timeout=30)` |
| `func sum(xs ...int)` | `def total(*xs):` |
| `sum(xs...)` | `total(*xs)` |
| `func f() (int, error)` | `return value` or raise; tuples for several values |
| `func(x int) int { return x * 2 }` | `lambda x: x * 2` |
| closure over a loop variable (fixed per iteration since Go 1.22) | closure sees the last value; bind with `i=i` |
