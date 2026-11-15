---
from: python
status: draft
concept: "Choosing patterns"
from_way: "Writing Java-style patterns in Python: abstract base classes, factories and singleton metaclasses."
native_way: "Choosing from the forces and using what the language gives: functions, modules, decorators, context managers, dataclasses and protocols; classes and patterns only where state and structure demand them."
sensei_tip: "A useful check in Python reviews: if a class has one method besides `__init__`, it is probably a function, and the pattern you built around it can go."
---

| Problem | Python choice |
| --- | --- |
| varying algorithm | pass a function, a dict of functions |
| cross-cutting behaviour | function decorators, context managers |
| closed variants | dataclasses + `match` |
| reacting to facts | signals, callbacks |
| foreign model | adapter module behind a Protocol |
| combinable rules | predicate functions, ORM `Q` objects |
