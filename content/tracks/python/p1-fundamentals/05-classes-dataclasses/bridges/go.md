---
from: go
status: draft
concept: "Structs, methods and constants"
from_way: "Structs with exported and unexported fields, methods on value or pointer receivers, constructor functions such as NewAccount, embedding instead of inheritance, and iota constants."
native_way: "Classes and dataclasses with methods, @classmethod constructors, inheritance when it helps but composition by default, and Enum classes."
sensei_tip: "A Go struct with a NewX function maps well to a dataclass with __post_init__ validation. Python has no value receivers: every method sees the same object, like a pointer receiver."
---

| Go | Python |
| --- | --- |
| `type Order struct { ID string }` | `@dataclass class Order: id: str` |
| `func NewOrder(id string) (*Order, error)` | `Order(id)` with checks in `__post_init__`, raising `ValueError` |
| `func (o *Order) Pay()` | `def pay(self):` |
| lower-case unexported fields | `_internal` by convention |
| embedding | composition (or inheritance) |
| `const ( Open Status = iota )` | `class Status(Enum): OPEN = auto()` |
| `String() string` | `__repr__` / `__str__` |
