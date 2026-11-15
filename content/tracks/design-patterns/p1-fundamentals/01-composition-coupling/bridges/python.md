---
from: python
status: draft
concept: "Composition, coupling and cohesion"
from_way: "Mixins stacked in multiple inheritance (`class View(LoginRequired, Cached, Logged, Base)`) and module-level globals shared by everything."
native_way: "Objects that hold other objects, functions passed in, and protocols for roles; mixins only when the method resolution order is simple and documented."
sensei_tip: "Python's MRO makes deep mixin stacks order-sensitive: swapping two base classes changes behaviour. Composition makes the order explicit in one constructor."
---

| Python | Idea |
| --- | --- |
| `self.store = store` | composition |
| multiple inheritance and mixins | inheritance (use sparingly) |
| `global` and module-level mutable state | common coupling |
| keyword-only flags `save(x, compress=True)` | control coupling, made explicit |
| `__getattr__` forwarding | delegation |
