---
from: python
status: draft
concept: "Creational patterns"
from_way: "Java-style builders and a `Singleton` metaclass."
native_way: "Keyword arguments with defaults replace most builders, `@dataclass(frozen=True)` with `__post_init__` validation gives value objects, `dataclasses.replace` gives copies, and modules are singletons."
sensei_tip: "`dataclasses.replace(settings, retries=5)` is the prototype pattern for frozen dataclasses: a copy with changes, no clone needed."
---

| Python | Pattern |
| --- | --- |
| `def __init__(self, url, *, method="GET", timeout=10)` | builder (keyword arguments) |
| `@classmethod def from_dict(cls, d)` | static factory |
| `@dataclass(frozen=True)` + `__post_init__` | validated value object |
| a module-level instance | singleton |
| `dataclasses.replace(obj, x=1)`, `copy.copy` | prototype |
| `sys.intern`, small int cache | flyweight |
