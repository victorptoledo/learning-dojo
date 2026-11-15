# Protocols, ABCs, Special Methods and Descriptors

The first phase showed that `len(x)` calls `x.__len__()`. This lesson goes the whole way: how
to describe "anything that behaves like X" for the type checker, how to make your own types
work with every operator and built-in, and the machinery (descriptors) that `property`,
methods and ORMs are built on.

## Protocols: structural typing

A `Protocol` lists methods and attributes. Any class that has them **matches**, without
inheriting or registering:

```python
from typing import Protocol

class Repository[T](Protocol):
    def get(self, key: str) -> T | None: ...
    def put(self, key: str, value: T) -> None: ...

class MemoryRepo:                       # no base class
    def __init__(self) -> None:
        self.items: dict[str, Order] = {}
    def get(self, key: str) -> Order | None:
        return self.items.get(key)
    def put(self, key: str, value: Order) -> None:
        self.items[key] = value

def archive(repo: Repository[Order]) -> None: ...
archive(MemoryRepo())                   # accepted by mypy and pyright
```

Protocols are the natural way to describe **dependencies** (a clock, a repository, an HTTP
client): the code under test never imports the real implementation, and a fake in a test
matches by shape. `@runtime_checkable` allows `isinstance(x, Repository)`, but it only checks
that the methods exist, not their signatures.

## Abstract base classes: nominal typing

An ABC is a base class you **inherit** from. It can force subclasses to implement methods and
can provide shared code:

```python
from abc import ABC, abstractmethod

class Exporter(ABC):
    @abstractmethod
    def write_row(self, row: dict) -> None: ...

    def write_all(self, rows: Iterable[dict]) -> None:     # shared implementation
        for row in rows:
            self.write_row(row)

Exporter()   # TypeError: can't instantiate abstract class
```

| Use a Protocol when | Use an ABC when |
| --- | --- |
| You describe what a caller needs | You provide a framework with shared behaviour |
| Implementations live in other code (or third-party code) | Implementations are your subclasses |
| Checking happens in the type checker | You want `isinstance` checks and errors at instantiation |

### collections.abc mixins

The ABCs in `collections.abc` write most of a container for you. Implement the abstract
methods, inherit the rest:

```python
from collections.abc import Sequence

class Playlist(Sequence[str]):
    def __init__(self, songs: list[str]) -> None:
        self._songs = list(songs)
    def __getitem__(self, index):          # abstract
        return self._songs[index]
    def __len__(self) -> int:              # abstract
        return len(self._songs)

p = Playlist(["a", "b", "a"])
"a" in p, p.count("a"), p.index("b"), list(reversed(p))   # all inherited
```

`Mapping` needs `__getitem__`, `__iter__` and `__len__`; `MutableMapping` adds `__setitem__`
and `__delitem__`; `Set`, `MutableSequence` and `Iterator` work the same way.

## Special methods in depth

| Behaviour | Methods |
| --- | --- |
| Text | `__repr__` (unambiguous, for developers), `__str__` (for users), `__format__` (for `f"{x:spec}"`) |
| Equality and hashing | `__eq__`, `__hash__` |
| Ordering | `__lt__`, `__le__`, `__gt__`, `__ge__` (or one of them plus `functools.total_ordering`) |
| Arithmetic | `__add__`, `__radd__` (reflected), `__iadd__` (in place), `__mul__`, `__neg__`, ... |
| Truth and size | `__bool__`, `__len__` |
| Containers | `__getitem__`, `__setitem__`, `__delitem__`, `__contains__`, `__iter__`, `__reversed__` |
| Calling | `__call__` |
| Attributes | `__getattr__` (only for missing attributes), `__setattr__`, `__getattribute__` (every access: rarely needed) |

### Operators and NotImplemented

For `a + b`, Python calls `a.__add__(b)`. If that returns the special value **`NotImplemented`**,
it tries `b.__radd__(a)`; if that also returns `NotImplemented`, it raises `TypeError`. Return
it (do not raise) for types you do not support:

```python
@dataclass(frozen=True)
class Money:
    cents: int
    currency: str

    def __add__(self, other: object) -> "Money":
        if not isinstance(other, Money):
            return NotImplemented
        if other.currency != self.currency:
            raise ValueError(f"cannot add {self.currency} and {other.currency}")
        return Money(self.cents + other.cents, self.currency)

    def __radd__(self, other: object) -> "Money":
        if other == 0:                      # sum() starts from 0
            return self
        return self.__add__(other)

    def __format__(self, spec: str) -> str:
        return f"{format(self.cents / 100, spec)} {self.currency}"

sum([Money(150, "EUR"), Money(250, "EUR")])   # Money(cents=400, currency='EUR')
f"{Money(1999, 'EUR'):.2f}"                    # '19.99 EUR'
```

### Equality and hashing

Objects that compare equal **must** have equal hashes, or dicts and sets break. Defining
`__eq__` in a class sets `__hash__` to `None` (unhashable) unless you also define it. Hash
only immutable state: `hash((self.cents, self.currency))`, or let `@dataclass(frozen=True)`
do it.

## Descriptors

A **descriptor** is an object, stored on a class, with `__get__` (and optionally `__set__`
or `__delete__`). Attribute access on instances is routed through it. That is how `property`,
methods, `classmethod`, `staticmethod`, `functools.cached_property` and ORM fields work:

```python
class Positive:
    def __set_name__(self, owner, name: str) -> None:
        self.private = f"_{name}"                       # called once, when the class is created

    def __get__(self, obj, objtype=None):
        if obj is None:
            return self                                  # accessed on the class itself
        return getattr(obj, self.private)

    def __set__(self, obj, value) -> None:
        if value <= 0:
            raise ValueError(f"{self.private[1:]} must be positive")
        setattr(obj, self.private, value)

class Order:
    quantity = Positive()
    price = Positive()

    def __init__(self, quantity: int, price: int) -> None:
        self.quantity = quantity                         # goes through Positive.__set__
        self.price = price
```

Methods are descriptors too: a function stored on a class has a `__get__` that returns a
**bound method** with `self` filled in. That is the whole secret of `self`.

## `__slots__`

By default each instance stores its attributes in a `__dict__`. Declaring `__slots__` (or
`@dataclass(slots=True)`) stores them in fixed slots instead: noticeably less memory per
instance, slightly faster access, and assigning an attribute that is not declared raises
`AttributeError`. Worth it for classes with millions of instances.

## Hooks into class creation

`__init_subclass__` runs on the base class whenever it is subclassed. It covers most of what
people once wrote metaclasses for, such as registries of plugins:

```python
class Exporter:
    registry: dict[str, type["Exporter"]] = {}

    def __init_subclass__(cls, *, format: str, **kwargs) -> None:
        super().__init_subclass__(**kwargs)
        Exporter.registry[format] = cls

class CsvExporter(Exporter, format="csv"): ...
class JsonExporter(Exporter, format="json"): ...

Exporter.registry["csv"]   # <class 'CsvExporter'>
```

**Metaclasses** (`class Model(metaclass=ModelMeta)`) customise how classes themselves are
built. Django models and some ORMs use them; in application code, a class decorator or
`__init_subclass__` is nearly always enough.

## Summary

- `Protocol` = structural, for dependencies and the type checker; ABC = nominal, with shared
  code and instantiation checks; `collections.abc` mixins build containers.
- Special methods make your types work with operators and built-ins; return `NotImplemented`
  for unknown operands; keep `__eq__` and `__hash__` consistent.
- Descriptors (`__get__`, `__set__`, `__set_name__`) power `property`, methods and ORM
  fields.
- `__slots__` saves memory; `__init_subclass__` replaces most metaclasses.

## Further reading

- PEP 544, protocols and structural subtyping: <https://peps.python.org/pep-0544/>
- `collections.abc`, abstract base classes for containers: <https://docs.python.org/3/library/collections.abc.html>
- The Language Reference, "Data model" (special method names): <https://docs.python.org/3/reference/datamodel.html#special-method-names>
- Descriptor HOWTO: <https://docs.python.org/3/howto/descriptor.html>
- The typing specification, "Protocols": <https://typing.python.org/en/latest/spec/protocol.html>
- roadmap.sh, Python topic "Object-Oriented Programming": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/object-oriented-programming@P_Di-XPSDITmU3xKQew8G.md>
