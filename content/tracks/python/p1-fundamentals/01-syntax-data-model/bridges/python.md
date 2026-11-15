---
from: python
status: draft
concept: "Modern idioms for everyday syntax"
from_way: "% formatting or str.format, len(x) == 0 checks, range(len(xs)) loops, long if/elif chains on dictionaries and type(x) == list comparisons."
native_way: "f-strings with format specs and =, truthiness checks, enumerate and zip(strict=True), the walrus operator and match on the structure of data."
sensei_tip: "ruff's pyupgrade rules (UP) rewrite most old formatting and syntax for you; run uvx ruff check --select UP --fix on an old code base and read the diff."
---

| Older habit | Python 3.15 |
| --- | --- |
| `"%s owes %.2f" % (name, x)` / `"{}".format(x)` | `f"{name} owes {x:.2f}"`, `f"{x=}"` |
| `if len(items) == 0:` | `if not items:` |
| `for i in range(len(xs)):` | `for i, x in enumerate(xs):` |
| `zip(a, b)` that silently truncates | `zip(a, b, strict=True)` |
| `line = f.readline()` then `while line:` | `while (line := f.readline()):` |
| `if d["type"] == "click" and "x" in d:` chains | `match d: case {"type": "click", "x": x}:` |
| `type(x) == list` | `isinstance(x, list)` or a `match` class pattern |
