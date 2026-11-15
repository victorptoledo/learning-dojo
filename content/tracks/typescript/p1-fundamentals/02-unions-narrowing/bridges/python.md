---
from: python
status: draft
concept: "Unions and narrowing like mypy, but stricter"
from_way: "Union types (str | int) narrowed by isinstance, Literal types, match statements, TypeGuard and TypeIs, assert_never."
native_way: "The same ideas with typeof, in and tag checks; strict mode always checks null and undefined, and narrowing is part of every check."
sensei_tip: "You already know this from mypy: isinstance becomes typeof or instanceof, Literal becomes a literal type, TypeIs becomes `value is T`, and typing.assert_never is a one-line helper."
---

| Python | TypeScript |
| --- | --- |
| `str \| int` | `string \| number` |
| `Literal["north", "south"]` | `"north" \| "south"` |
| `isinstance(x, str)` | `typeof x === "string"` |
| `def is_cat(a) -> TypeIs[Cat]` | `function isCat(a): a is Cat` |
| `typing.assert_never(x)` | `function assertNever(x: never): never` |
| `Optional[str]` | `string \| undefined` |
| `object` | `unknown` |
