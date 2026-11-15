---
from: python
status: draft
concept: "Specification"
from_way: "Query methods per combination and the same conditions repeated in Django `filter()` calls across views."
native_way: "Functions returning booleans for in-memory rules, Django `Q` objects or SQLAlchemy expressions as composable query specifications, and dataclasses with interpreters when you need both."
sensei_tip: "Django's `Q(active=True) & ~Q(country='PT')` is a specification as data: the ORM interprets it into SQL with parameters, never by string concatenation."
---

| Python | Specification idea |
| --- | --- |
| `lambda c: c.active` | rule as a function |
| `all(rule(c) for rule in rules)` | and-composition |
| Django `Q(...) & Q(...)`, `~Q(...)` | specs as query data |
| SQLAlchemy `and_(...)`, `or_(...)` | specs as expressions |
| dataclasses + `match` interpreters | specification as data |
