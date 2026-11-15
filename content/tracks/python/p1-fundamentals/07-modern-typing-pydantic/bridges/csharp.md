---
from: csharp
status: draft
concept: "Static typing, generics and model validation"
from_way: "Compile-time types, nullable reference types, generics with constraints (where T : IComparable), interfaces, and DataAnnotations or FluentValidation on DTOs."
native_way: "Hints checked by an external tool, X | None for nullability, [T: Bound] generics, Protocols for structural interfaces, and Pydantic for validation."
sensei_tip: "Nullable reference types and X | None are the same idea: the checker forces you to handle None before use. Turn on strict mode early, like <Nullable>enable</Nullable>."
---

| C# | Python |
| --- | --- |
| `string?` with nullable enabled | `str \| None` with a strict checker |
| `T First<T>(IReadOnlyList<T> xs)` | `def first[T](xs: Sequence[T]) -> T:` |
| `where T : IComparable<T>` | `[T: Comparable]`, where `Comparable` is a Protocol with `__lt__` |
| `IEnumerable<double>` parameter | `Iterable[float]` |
| `interface IClosable` | `class SupportsClose(Protocol)` |
| `[Range(1, int.MaxValue)] int Qty` | `qty: int = Field(ge=1)` |
| `JsonSerializer.Deserialize<Order>(json)` | `Order.model_validate_json(json)` |
