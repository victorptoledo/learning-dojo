---
from: python
status: draft
concept: "TypeVar, bounds and PEP 695 generics"
from_way: "def first[T](items: list[T]) -> T | None (PEP 695), bounds with [T: Sized], Literal for keys, and no way to type 'a key of this TypedDict' generically."
native_way: "<T> on functions and types, extends for bounds, keyof and T[K] for keys and their value types, const type parameters for literals."
sensei_tip: "Python's new [T] syntax maps directly to <T>. What Python lacks is keyof: in TypeScript, get(obj, key) can be typed so only real keys compile and the result has that key's type."
---

| Python | TypeScript |
| --- | --- |
| `def first[T](items: list[T]) -> T \| None` | `function first<T>(items: T[]): T \| undefined` |
| `[T: Sized]` (bound) | `<T extends { length: number }>` |
| `class Box[T]:` | `class Box<T>` / `type Box<T> = { value: T }` |
| `Literal["id", "name"]` written by hand | `keyof User` |
| `TypedDict` value types | `User["name"]` |
