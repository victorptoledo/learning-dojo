---
from: java
status: draft
concept: "Overloads, varargs and lambdas"
from_way: "Overloaded methods or builders for optional arguments, String... varargs, functional interfaces for lambdas, and lambdas that may capture only effectively final variables."
native_way: "One function with defaults and keyword-only parameters, *args and **kwargs, functions as plain values, and closures that can rebind outer variables with nonlocal."
sensei_tip: "Replace a family of overloads or a builder with one function that has keyword-only defaults: connect(host, *, port=5432, ssl=True) is the whole API."
---

| Java | Python |
| --- | --- |
| `connect(host)`, `connect(host, port)` overloads | `def connect(host, port=5432):` |
| `Builder().ssl(false).build()` | `connect("db", ssl=False)` |
| `void log(String... parts)` | `def log(*parts):` |
| `Function<String, Integer>` | `Callable[[str], int]`, or just pass the function |
| `String::length` | `len` |
| effectively final captures | closures with `nonlocal` |
| `static int counter` | a closure, an object or a module variable |
