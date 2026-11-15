---
from: go
status: draft
concept: "Static types, implicit interfaces and decoding"
from_way: "Types checked by the compiler, interfaces satisfied implicitly by any type with the methods, generics with [T any] and constraints, and json.Unmarshal plus hand-written validation."
native_way: "Hints checked by mypy or pyright, Protocols satisfied implicitly like Go interfaces, generics with [T], and Pydantic models that decode and validate in one step."
sensei_tip: "A typing.Protocol is a Go interface: no implements clause, any type with the right methods fits. Python even uses the same bracket syntax for type parameters."
---

| Go | Python |
| --- | --- |
| `go vet` / the compiler | `mypy --strict` / `pyright` |
| `type Closer interface { Close() error }` | `class SupportsClose(Protocol): def close(self) -> None: ...` |
| `func First[T any](xs []T) T` | `def first[T](xs: Sequence[T]) -> T:` |
| `[T constraints.Ordered]` | `[T: (int, float, str)]` or a bound |
| `*string` for optional | `str \| None` |
| `json.Unmarshal` + `validate` tags | `Order.model_validate_json(data)` |
| `any` | `object` (or `Any`, which turns checking off) |
