---
from: python
status: draft
concept: "Gradual typing and its gaps"
from_way: "Gradual typing: Any is the escape hatch, cast() is unchecked, mypy has strict flags (--strict, disallow-any-*), TypeVar(covariant=True) or PEP 695 inferred variance."
native_way: "The same gradual model: any and as are unchecked, strict options close specific holes, and variance is inferred or annotated with in and out."
sensei_tip: "Both languages chose usefulness over soundness. The mypy habit of turning on --strict and banning Any translates directly to strict, noUncheckedIndexedAccess and typescript-eslint's no-unsafe rules."
---

| Python | TypeScript |
| --- | --- |
| `Any` | `any` |
| `typing.cast(User, x)` | `x as User` |
| `mypy --strict` | `"strict": true` + extra flags |
| `TypeVar("T", covariant=True)` | `interface P<out T>` |
| `Sequence[T]` (covariant) vs `list[T]` | `readonly T[]` vs `T[]` |
| `mypy --timing-stats` | `tsc --extendedDiagnostics` |
