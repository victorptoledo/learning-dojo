---
from: python
status: draft
concept: "Typed duck typing and lighter class machinery"
from_way: "Untyped duck typing documented in prose, ABCs used only to get isinstance checks, metaclasses for registries, __cmp__ (Python 2), hand-written comparison methods and hasattr checks everywhere."
native_way: "typing.Protocol for duck typing the checker understands, ABCs when sharing code, __init_subclass__ for registries, total_ordering or dataclass(order=True), and NotImplemented in operators."
sensei_tip: "Before writing a metaclass, try __init_subclass__ or a class decorator; before writing an ABC just for typing, try a Protocol."
---

| Older habit | Python 3.15 |
| --- | --- |
| "pass anything with a .read() method" in a docstring | `class SupportsRead(Protocol): def read(self) -> bytes: ...` |
| `class Meta(type)` to register subclasses | `def __init_subclass__(cls, **kwargs)` |
| `__cmp__` / `cmp=` (Python 2) | `__lt__` + `functools.total_ordering` |
| writing all six comparisons by hand | `@dataclass(order=True)` or `total_ordering` |
| `raise TypeError` inside `__add__` | `return NotImplemented` |
| `if hasattr(obj, "close"):` | a Protocol in the signature, checked statically |
| `__slots__` written by hand | `@dataclass(slots=True)` |
