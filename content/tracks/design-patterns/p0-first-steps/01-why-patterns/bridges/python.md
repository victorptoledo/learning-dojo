---
from: python
status: draft
concept: "Design patterns"
from_way: "Translating Java class diagrams line by line: an abstract base class, a factory class and a singleton metaclass for everything."
native_way: "First-class functions, modules, duck typing and decorators make many patterns a few lines or unnecessary; the structural ones (adapter, facade, repository) stay."
sensei_tip: "A Python module is already a singleton, a function is already a strategy, and `@functools.cache` is a decorator in both senses. Reach for a class when there is state to keep."
---

| Python | Pattern |
| --- | --- |
| passing a function (`key=len`) | strategy |
| a module with top-level objects | singleton |
| `@functools.wraps` decorators | decorator (function level) |
| `__iter__` and generators | iterator |
| `typing.Protocol` | program to an interface |
| `contextlib.contextmanager` | template method around a block |
