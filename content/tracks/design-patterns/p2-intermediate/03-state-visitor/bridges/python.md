---
from: python
status: draft
concept: "State, visitor, composite and template method"
from_way: "String states compared in if/elif chains and a visitor class with a visit method per node type."
native_way: "Dataclasses for states and nodes, `match` statements with class patterns for visitor-style operations, and `functools.singledispatch` for open dispatch."
sensei_tip: "Python's `match` is not exhaustive by default; type checkers such as mypy and pyright can check exhaustiveness over a `Union` of dataclasses if you add `case _: assert_never(x)`."
---

| Python | Pattern |
| --- | --- |
| `@dataclass` states, `Union[...]` | state |
| `match node: case Circle(r=r): ...` | visitor |
| `functools.singledispatch` | open visitor |
| `ast.NodeVisitor` | classic visitor |
| dataclasses holding lists of nodes | composite |
| `__iter__`, generators | iterator |
