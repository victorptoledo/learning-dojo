---
from: javascript
status: draft
concept: "TypeScript types and runtime validation"
from_way: "TypeScript annotations erased at compile time, tsc --strict, structural interfaces, generics with <T>, literal unions, and zod schemas to validate data at run time."
native_way: "Annotations ignored at run time, mypy or pyright --strict, Protocols and TypedDict, generics with [T], Literal, and Pydantic models to validate data."
sensei_tip: "The model is the same as TypeScript plus zod: hints are erased (ignored), the checker runs separately, and a schema library validates at the edges. Pydantic is zod with classes."
---

| TypeScript | Python |
| --- | --- |
| `tsc --strict` | `mypy --strict` / `pyright` |
| `string \| undefined` | `str \| None` |
| `"r" \| "w"` | `Literal["r", "w"]` |
| `interface User { id: number }` (structural) | `class User(TypedDict)` or a `Protocol` |
| `function first<T>(xs: T[]): T` | `def first[T](xs: Sequence[T]) -> T:` |
| `type Pair<T> = [T, T]` | `type Pair[T] = tuple[T, T]` |
| `z.object({ qty: z.number().min(1) }).parse(body)` | `Item.model_validate(body)` with `qty: int = Field(ge=1)` |
| `as Foo` | `cast(Foo, x)` |
