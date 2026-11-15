---
from: python
status: draft
concept: "From typing imports to modern annotations"
from_way: "from typing import List, Dict, Optional, Union, TypeVar('T') with Generic[T], type comments, and Pydantic v1 with .dict(), .parse_obj() and @validator."
native_way: "Built-in generics (list[int]), X | None, PEP 695 [T] parameters and type aliases, collections.abc for abstract types, and Pydantic v2's model_* API."
sensei_tip: "ruff's UP rules rewrite List[int] to list[int] and Optional[X] to X | None automatically. For Pydantic, the migration guide maps every v1 method to its v2 name."
---

| Older habit | Python 3.15 |
| --- | --- |
| `List[int]`, `Dict[str, int]` | `list[int]`, `dict[str, int]` |
| `Optional[str]`, `Union[int, str]` | `str \| None`, `int \| str` |
| `T = TypeVar("T")` + `class Box(Generic[T])` | `class Box[T]:` |
| `Pair = Tuple[T, T]` | `type Pair[T] = tuple[T, T]` |
| `from typing import Iterable, Callable` | `from collections.abc import Iterable, Callable` |
| `x = []  # type: List[int]` | `x: list[int] = []` |
| Pydantic v1 `.dict()`, `.parse_obj()`, `@validator` | v2 `.model_dump()`, `.model_validate()`, `@field_validator` |
