---
from: csharp
status: draft
concept: "Optional, named and params arguments"
from_way: "Optional parameters with compile-time constant defaults, named arguments, params arrays, Func and Action delegates, and lambdas capturing variables."
native_way: "Defaults of any value evaluated once, keyword arguments that can be required by name, *args and **kwargs, plain functions as values and closures."
sensei_tip: "C# named arguments are optional at the call site; Python can force them with *, which is often what you wanted the C# API to do."
---

| C# | Python |
| --- | --- |
| `void Connect(string host, int port = 5432)` | `def connect(host, port=5432):` |
| `Connect("db", ssl: false)` | `connect("db", ssl=False)` |
| `params string[] parts` | `*parts` |
| `Func<string, int>` | `Callable[[str], int]` |
| `x => x * 2` | `lambda x: x * 2` |
| local functions capturing variables | nested functions (closures) with `nonlocal` |
| method group `int.Parse` | the function object `int` |
