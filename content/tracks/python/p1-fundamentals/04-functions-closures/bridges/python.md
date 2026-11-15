---
from: python
status: draft
concept: "Explicit signatures instead of loose ones"
from_way: "def f(*args, **kwargs) everywhere with kwargs.get lookups, boolean flags passed positionally, global counters, and classes with one method just to hold state."
native_way: "Keyword-only parameters after *, positional-only before /, None defaults for mutable values, closures or functools.partial, and type hints on every parameter."
sensei_tip: "A bare * in a signature is the cheapest readability win in Python: f(data, True, False) becomes impossible, and calls document themselves."
---

| Older habit | Python 3.15 |
| --- | --- |
| `def f(a, b, flag=False)` called as `f(x, y, True)` | `def f(a, b, *, flag=False)` |
| `def f(**kwargs): timeout = kwargs.get("timeout", 5)` | `def f(*, timeout: float = 5):` |
| `def f(items=[])` | `def f(items: list[str] \| None = None)` |
| `global counter` | a closure with `nonlocal`, or an object |
| `lambda x: int(x, 16)` | `partial(int, base=16)` |
| no way to stop `f(source=...)` | `def f(source, /):` |
