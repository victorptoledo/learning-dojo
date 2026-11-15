# Classes, Dataclasses and Enums

Python's classes are simple: no access modifiers, no interfaces you must declare, no
overloading. What you get instead is a small set of conventions, and **dataclasses**, which
write the boring parts of a class for you.

## A plain class

```python
class Account:
    bank = "Dojo Bank"                    # class attribute: shared by all instances

    def __init__(self, owner: str, balance: int = 0) -> None:
        self.owner = owner                # instance attributes, created on self
        self._balance = balance           # leading underscore: internal, by convention

    def deposit(self, cents: int) -> None:
        if cents <= 0:
            raise ValueError("deposit must be positive")
        self._balance += cents

    @property
    def balance(self) -> int:             # read like an attribute: account.balance
        return self._balance

    @classmethod
    def from_csv(cls, line: str) -> "Account":   # an alternative constructor
        owner, cents = line.split(",")
        return cls(owner, int(cents))

    def __repr__(self) -> str:
        return f"Account(owner={self.owner!r}, balance={self._balance})"
```

| Feature | What to know |
| --- | --- |
| `self` | The instance, passed explicitly as the first parameter of every method |
| `_name` | "Internal, do not touch": a convention, not enforced. `__name` triggers name mangling, rarely needed |
| `@property` | A method read like an attribute; add a `@balance.setter` only if writing makes sense |
| `@classmethod` | Receives the class (`cls`): alternative constructors, and it works for subclasses |
| `@staticmethod` | A plain function namespaced in the class; often a module-level function is better |
| `__repr__` | What you see in the REPL, logs and debuggers: always worth writing (or let a dataclass do it) |

Start with public attributes. If you later need validation or a computed value, turn the
attribute into a property: callers do not change, so there is no reason for Java-style
getters and setters up front.

## Inheritance and composition

```python
class SavingsAccount(Account):
    def __init__(self, owner: str, rate: float) -> None:
        super().__init__(owner)           # run the parent's initialiser
        self.rate = rate
```

Python supports multiple inheritance, resolved by the **method resolution order**
(`SavingsAccount.__mro__`); `super()` follows it. In application code keep hierarchies flat:
prefer **composition** (an object that holds others) and protocols (a later lesson) to
inheritance chains.

## Dataclasses

Most classes are mainly data. `@dataclass` generates `__init__`, `__repr__` and `__eq__` from
annotated fields:

```python
from dataclasses import dataclass, field

@dataclass
class Order:
    id: str
    customer: str
    lines: list[str] = field(default_factory=list)   # never `= []`
    paid: bool = False

order = Order("o-1", "ada")
order                       # Order(id='o-1', customer='ada', lines=[], paid=False)
order == Order("o-1", "ada")  # True: compares the fields
```

Fields with defaults must come after fields without. A mutable default must use
`default_factory`, or each instance would share one list (dataclasses refuse `= []` with an
error for exactly that reason).

### Validation with `__post_init__`

`__post_init__` runs after the generated `__init__`. Validate and normalise there, so an
invalid object can never exist:

```python
@dataclass
class Email:
    value: str

    def __post_init__(self) -> None:
        self.value = self.value.strip().lower()
        if "@" not in self.value:
            raise ValueError(f"not an email: {self.value!r}")
```

### Options

| Option | Effect |
| --- | --- |
| `frozen=True` | Assigning a field raises `FrozenInstanceError`; instances are hashable (usable in sets and as keys) |
| `order=True` | Generates `<`, `<=`, `>`, `>=` comparing fields as a tuple, in declaration order |
| `slots=True` | Uses `__slots__`: less memory, faster attribute access, no accidental new attributes |
| `kw_only=True` | Every field must be passed by name |

A frozen dataclass is the Python value object. "Change" it by making a modified copy:

```python
from dataclasses import replace

@dataclass(frozen=True, slots=True)
class Point:
    x: float
    y: float

p = Point(1, 2)
q = replace(p, x=5)        # Point(x=5, y=2); p is unchanged (copy.replace(p, x=5) also works since 3.13)
```

In a frozen dataclass's `__post_init__`, `self.x = ...` is refused too; normalise with
`object.__setattr__(self, "x", value)`.

> [!NOTE]
> `eq=True` (the default) without `frozen=True` sets `__hash__` to `None`: a mutable object
> that compares by value must not be a dict key, because its hash could change.

## Enums

An enum is a fixed set of named values. `StrEnum` (3.11+) members are also strings, which
makes them easy to store and print:

```python
from enum import Enum, StrEnum, auto

class Status(StrEnum):
    OPEN = auto()       # "open"
    FROZEN = auto()     # "frozen"
    CLOSED = auto()

Status("open") is Status.OPEN    # look up by value
Status.OPEN == "open"            # True, because it is a str
[s.name for s in Status]         # ['OPEN', 'FROZEN', 'CLOSED']

class Planet(Enum):
    EARTH = (5.97e24, 6.37e6)
    MARS = (6.42e23, 3.39e6)

    def __init__(self, mass: float, radius: float) -> None:
        self.mass, self.radius = mass, radius
```

Enums pair well with `match`: `case Status.OPEN:` compares by value because the pattern is a
dotted name.

## Choosing a shape for data

| Use | When |
| --- | --- |
| `tuple` / `NamedTuple` | Small, immutable, positional records; `NamedTuple` adds names |
| `@dataclass` | Your own data types: the default choice |
| `@dataclass(frozen=True)` | Values: money, points, ids, events |
| Pydantic `BaseModel` | Data that crosses a boundary and must be parsed and validated (next lesson) |
| a plain class | Objects with behaviour and hidden state: a connection, a cache |

## Summary

- Classes: `__init__`, `self`, `_internal` by convention, properties instead of getters,
  `@classmethod` constructors and a `__repr__`.
- Flat hierarchies and composition; `super()` follows the MRO.
- `@dataclass` writes `__init__`, `__repr__` and `__eq__`; `default_factory` for mutable
  defaults; validate in `__post_init__`; `frozen`, `order`, `slots`, `kw_only`; `replace`.
- `Enum` and `StrEnum` for fixed sets of values.

## Further reading

- The Python Tutorial, "Classes": <https://docs.python.org/3/tutorial/classes.html>
- `dataclasses`: <https://docs.python.org/3/library/dataclasses.html>
- Enum HOWTO: <https://docs.python.org/3/howto/enum.html>
- PEP 557, data classes: <https://peps.python.org/pep-0557/>
- The descriptor behind `@property` (HOWTO): <https://docs.python.org/3/howto/descriptor.html>
- roadmap.sh, Python topic "Classes": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/classes@AqwzR8dZKLQIoj_6KKZ3t.md>
