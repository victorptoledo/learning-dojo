---
from: python
status: draft
concept: "SOLID principles"
from_way: "A module of loosely related functions that import the database client and call `datetime.now()` directly."
native_way: "Functions and classes grouped by reason to change, dependencies passed in as parameters, and `typing.Protocol` for the small interfaces callers need."
sensei_tip: "Duck typing makes interface segregation natural: a function that only calls `.read()` works with any object that has it. Write the Protocol to document that, not to build a hierarchy."
---

| Python | SOLID idea |
| --- | --- |
| one module per concern | single responsibility |
| a dict of functions or a plugin entry point | open/closed |
| overriding a method to `raise NotImplementedError` | a Liskov warning sign |
| `typing.Protocol` with one method | interface segregation |
| passing `now: Callable[[], datetime]` | dependency inversion |
