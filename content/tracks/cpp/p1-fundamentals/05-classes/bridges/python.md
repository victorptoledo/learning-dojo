---
from: python
status: draft
concept: "Classes and objects"
from_way: "class with __init__, self, properties, dataclasses, and attributes anyone can change."
native_way: "class with constructors and private members, const member functions, structs as plain data, and defaulted comparisons."
sensei_tip: "A @dataclass maps to a C++ struct with `= default` comparisons; a class with rules maps to private members plus methods that check them."
---

| Python | C++ |
| --- | --- |
| `def __init__(self, owner):` | `explicit Account(std::string owner)` |
| `self.balance` | `balance_` (private member) |
| `@dataclass(order=True)` | `struct` + `auto operator<=>(...) const = default;` |
| `@property` | a `const` member function |
| `__eq__` | `operator==` |
| `__repr__` | `std::formatter` specialisation |
| `_private` by convention | `private:` enforced by the compiler |
