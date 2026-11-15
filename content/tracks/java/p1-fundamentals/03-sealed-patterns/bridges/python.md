---
from: python
status: draft
concept: "Closed sets of alternatives"
from_way: "Union types checked by mypy and match with class patterns."
native_way: "sealed interfaces with records and switch patterns that the compiler checks for exhaustiveness."
sensei_tip: "Python's match is not exhaustive; Java's switch over a sealed type is, so adding a new record breaks every switch that does not handle it."
---

| Python | Java 25 |
| --- | --- |
| Shape = Circle \| Square | sealed interface Shape permits Circle, Square |
| match s: case Circle(r=r): | case Circle(var r) -> |
| case _: | no default needed |
| assert_never(x) | the compiler's exhaustiveness check |
