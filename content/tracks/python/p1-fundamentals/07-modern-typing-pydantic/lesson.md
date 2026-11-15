# Type Hints, Generics, mypy and Pydantic v2

Python stays dynamically typed, but professional Python is **annotated**. Type hints document
every signature, let your editor complete and refactor safely, and let a type checker find
whole classes of bugs before the code runs. Where data enters from outside, **Pydantic**
turns the same hints into validation.

## Annotations are not checked at run time

```python
def greet(name: str) -> str:
    return "Hi " + name

greet(42)   # runs, and fails inside with TypeError; the hint did not stop it
```

The interpreter stores annotations and ignores them. Two kinds of tools use them:

| Tool | When | What it does |
| --- | --- | --- |
| A **type checker**: mypy, pyright (VS Code's Pylance) | Before running, in the editor and in CI | Proves the code is consistent with the hints |
| A **validator**: Pydantic, FastAPI | At run time, at the boundaries | Parses untrusted data into typed objects, or rejects it |

```text
uv add --dev mypy
uv run mypy --strict .
```

`--strict` turns on everything, including "every function must be annotated". Start new
projects strict; type old ones gradually (unannotated functions are not checked by default).

## The everyday vocabulary

```python
from collections.abc import Callable, Iterable, Mapping, Sequence
from typing import Any, Final, Literal, TypedDict

count: int = 0
names: list[str] = []
scores: dict[str, float] = {}
point: tuple[int, int] = (0, 0)
row: tuple[str, ...] = ("a", "b", "c")      # any length
maybe: str | None = None                    # PEP 604; Optional[str] is the old spelling
MAX_RETRIES: Final = 3
Mode = Literal["r", "w", "a"]               # only these values
on_done: Callable[[int], None]              # takes an int, returns nothing


class UserRow(TypedDict):                   # the shape of a dict, e.g. parsed JSON
    id: int
    email: str
```

Since 3.15 a `TypedDict` can be declared `closed=True` (no keys beyond the ones listed) or
with `extra_items=int` (any extra keys, with values of that type), from PEP 728.

**Accept abstract, return concrete.** Parameters should ask only for what the function uses,
and results should be specific:

```python
def average(values: Iterable[float]) -> float: ...     # lists, tuples, generators all work
def lookup(table: Mapping[str, int], key: str) -> int | None: ...
def top(items: Sequence[str], n: int) -> list[str]: ...
```

`Any` switches checking off for a value; `object` accepts anything but lets you do nothing
with it until you narrow it. Prefer `object`.

## Generics: the 3.12+ syntax

A **type parameter** in square brackets after the name makes a function or class generic
(PEP 695):

```python
def first[T](items: Sequence[T]) -> T:
    return items[0]

first([1, 2])        # the checker knows this is an int
first(["a"])         # ... and this a str

class Stack[T]:
    def __init__(self) -> None:
        self._items: list[T] = []

    def push(self, item: T) -> None:
        self._items.append(item)

    def pop(self) -> T:
        return self._items.pop()

type Pair[T] = tuple[T, T]                       # a generic type alias
def biggest[T: (int, float)](xs: list[T]) -> T:  # constrained to int or float
    return max(xs)
```

`[T: Hashable]` sets an upper bound. Since 3.13 a type parameter can have a default
(`class Box[T = str]`, PEP 696). Older code spells all of this with `TypeVar("T")` and
`Generic[T]`; it still works.

## Narrowing

A checker follows your control flow. After an `isinstance` test, a `None` check or a `match`
case, the type is narrower:

```python
def label(value: int | str | None) -> str:
    if value is None:
        return "-"
    if isinstance(value, int):
        return f"#{value}"     # value: int here
    return value.upper()       # value: str here
```

For exhaustive checks over enums or unions, end a `match` with
`case _: assert_never(value)`: adding a new member then becomes a type error where you
forgot to handle it. `typing.TypeIs` (3.13) lets your own predicate functions narrow, and
`cast(T, value)` overrides the checker when you know better (use it rarely).

`typing.Self` annotates methods that return their own class, such as builders and
alternative constructors.

## Protocols, a first look

A `Protocol` describes the methods a type must have, and any class with those methods
matches, without inheriting from it. It is duck typing that the checker understands:

```python
from typing import Protocol

class SupportsClose(Protocol):
    def close(self) -> None: ...

def shutdown(resources: Iterable[SupportsClose]) -> None:
    for resource in resources:
        resource.close()     # files, sockets, your own classes: all accepted
```

The protocols lesson in the advanced phase goes much further.

## Pydantic v2: types at the boundary

Hints do nothing against a bad JSON request. Pydantic models **parse and validate** at run
time, using the same annotations (its core is written in Rust and is fast):

```python
from typing import Literal
from pydantic import BaseModel, ConfigDict, Field, ValidationError, field_validator

class Item(BaseModel):
    sku: str = Field(min_length=1)
    qty: int = Field(ge=1)

class OrderIn(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    order_id: str
    items: list[Item]
    currency: Literal["EUR", "USD"]
    note: str | None = None

    @field_validator("order_id")
    @classmethod
    def upper_id(cls, value: str) -> str:
        return value.upper()

order = OrderIn.model_validate_json(
    '{"order_id": "a-17", "items": [{"sku": "TEA-01", "qty": "2"}], "currency": "EUR"}'
)
order.items[0].qty          # 2: the string "2" was coerced to int (lax mode)
order.model_dump()          # back to plain dicts, model_dump_json() to JSON

try:
    OrderIn.model_validate({"order_id": "x", "items": [{"sku": "", "qty": 0}], "currency": "BRL"})
except ValidationError as error:
    print(error.error_count())   # 3: every problem, with its location, at once
```

| Pydantic | Purpose |
| --- | --- |
| `model_validate(obj)` / `model_validate_json(text)` | Parse untrusted input |
| `model_dump()` / `model_dump_json()` | Serialise |
| `Field(ge=, max_length=, pattern=, default=)` | Constraints and metadata |
| `@field_validator`, `@model_validator` | Custom checks and normalisation |
| `ConfigDict(strict=True, extra="forbid", frozen=True)` | No coercion, no unknown keys, immutable |
| `TypeAdapter(list[int])` | Validate any type, not only models |

**Where each fits:** Pydantic at the edges (HTTP bodies, configuration, messages, files);
dataclasses for the domain objects inside, where data is already trusted and validation
in `__post_init__` is enough.

> [!NOTE]
> With `from __future__ import annotations`, or by default in Python 3.14 (PEP 649),
> annotations are evaluated lazily. Code that reads them at run time should use
> `typing.get_type_hints()` (or `annotationlib` in 3.14) instead of `__annotations__`.

## Summary

- Hints are documentation plus input for checkers; nothing is enforced at run time.
- `list[int]`, `X | None`, `Literal`, `TypedDict`, `Callable`; accept `Iterable`/`Mapping`,
  return concrete types.
- Generics with `def f[T]`, `class C[T]` and `type Alias[T] = ...`.
- Narrow with `isinstance`, `None` checks and `match`; `assert_never` for exhaustiveness.
- Run mypy or pyright in strict mode in CI; use Pydantic v2 to validate data from outside.

## Further reading

- `typing`, support for type hints: <https://docs.python.org/3/library/typing.html>
- The typing documentation and specification: <https://typing.python.org/en/latest/>
- PEP 695, type parameter syntax: <https://peps.python.org/pep-0695/>
- mypy, "Type hints cheat sheet": <https://mypy.readthedocs.io/en/stable/cheat_sheet_py3.html>
- Pydantic, "Models": <https://docs.pydantic.dev/latest/concepts/models/>
- roadmap.sh, Python topic "Typing": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/typing@o1wi39VnjnFfWIC8XcuAK.md>
