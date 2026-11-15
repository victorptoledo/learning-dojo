---
from: javascript
status: draft
concept: "Parameters, rest arguments and closures"
from_way: "Default parameters evaluated on every call, rest parameters, an options object for named arguments, arrow functions and closures that capture let bindings per iteration."
native_way: "Defaults evaluated once at def time, *args and **kwargs, real keyword arguments, lambda for one expression and closures that capture the variable, not a per-iteration copy."
sensei_tip: "Two differences bite JavaScript developers: Python defaults are evaluated once (so never use [] or {} as a default), and a loop variable is one variable for the whole loop, like var, not let."
---

| JavaScript | Python |
| --- | --- |
| `function f(a, b = 1)` (default per call) | `def f(a, b=1):` (default once) |
| `function f(...rest)` | `def f(*rest):` |
| `f({ ssl: false })` with destructuring | `f(ssl=False)` with keyword-only params |
| `(x) => x * 2` | `lambda x: x * 2` |
| `for (let i ...)` closures see each i | closures see the last i; bind with `i=i` |
| `fn.bind(null, 16)` | `functools.partial(fn, 16)` |
| `f(...args)` | `f(*args)` / `f(**kwargs)` |
