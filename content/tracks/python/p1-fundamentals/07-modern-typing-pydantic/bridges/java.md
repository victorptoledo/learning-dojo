---
from: java
status: draft
concept: "Static types, generics and bean validation"
from_way: "Types the compiler enforces, generics with <T> and wildcards, interfaces a class must implement, and Jakarta Bean Validation (@NotNull, @Min) on DTOs."
native_way: "Optional hints checked by mypy or pyright, generics with [T], Protocols matched by structure, and Pydantic models that validate at run time."
sensei_tip: "Run the type checker in CI as if it were the compiler: mypy --strict failing the build is how Python teams get Java-like safety. Pydantic is your Bean Validation plus Jackson in one."
---

| Java | Python |
| --- | --- |
| compiler errors | `mypy --strict` / pyright in CI |
| `<T> T first(List<T> xs)` | `def first[T](xs: Sequence[T]) -> T:` |
| `<T extends Comparable<T>>` | `[T: Comparable]`, where `Comparable` is a Protocol with `__lt__` |
| `List<? extends Number>` | `Sequence[float]` (read-only types are covariant) |
| `interface Closeable` + `implements` | `class SupportsClose(Protocol)`, no inheritance needed |
| `@NotNull @Min(1) int qty` | `qty: int = Field(ge=1)` |
| `objectMapper.readValue(json, Order.class)` | `Order.model_validate_json(json)` |
| `Optional<String>` | `str \| None` |
