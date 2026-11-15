---
from: python
status: draft
concept: "Classes, protocols and typed callables"
from_way: "Classes with _private by convention, Protocol for structural interfaces, Callable[[int], str] for function types, @overload in stubs."
native_way: "Interfaces are structural like Protocol; #fields are truly private; function types are written as arrows; overloads are signatures above one body, like @overload."
sensei_tip: "TypeScript's interface is Python's Protocol, and its overloads work like typing.overload. The surprise is #private: real privacy that Python never had."
---

| Python | TypeScript |
| --- | --- |
| `class Greeter(Protocol): def greet(self) -> str` | `interface Greeter { greet(): string }` |
| `Callable[[int, int], int]` | `(a: number, b: number) => number` |
| `@overload` signatures + implementation | Overload signatures + implementation |
| `self._secret` (convention) | `#secret` (enforced) |
| `@property` | `get initials()` |
| `def f(*args: str)` | `function f(...args: string[])` |
