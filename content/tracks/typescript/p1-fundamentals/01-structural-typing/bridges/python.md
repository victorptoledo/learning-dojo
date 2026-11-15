---
from: python
status: draft
concept: "Duck typing, checked by the compiler"
from_way: "Duck typing at runtime; typing.Protocol gives structural checks in mypy, while ordinary classes are nominal."
native_way: "Every object type is structural, like a Protocol; there is no separate nominal mode except brands and private fields."
sensei_tip: "If you liked Protocol, TypeScript is Protocol everywhere. The thing to unlearn is NewType: a type alias does not create a new type, use a brand."
---

| Python | TypeScript |
| --- | --- |
| `class Named(Protocol): name: str` | `interface Named { name: string }` |
| `TypedDict` with `NotRequired[str]` | `{ email?: string }` |
| `tuple[int, int]` | `[number, number]` |
| `dict[str, int]` | `Record<string, number>` |
| `NewType("UserId", str)` | `string & { readonly __brand: "UserId" }` |
| `Any` | `any` (avoid), `object` / `unknown` instead |
