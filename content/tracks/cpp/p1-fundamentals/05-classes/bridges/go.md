---
from: go
status: draft
concept: "Structs and methods"
from_way: "Structs with exported or unexported fields, methods with value or pointer receivers, constructors named NewX, == on comparable structs."
native_way: "Classes with access control, member functions (with this), real constructors, const member functions, and generated comparisons."
sensei_tip: "A Go method with a value receiver is like a C++ const member function; a pointer receiver is like a non-const one."
---

| Go | C++ |
| --- | --- |
| `type Account struct { balance int64 }` | `class Account { std::int64_t balance_; };` |
| `func NewAccount(...) (*Account, error)` | a constructor (throws) or a factory returning `std::expected` |
| `func (a Account) Balance()` | `std::int64_t balance() const` |
| `func (a *Account) Deposit()` | `void deposit(...)` |
| lowercase = unexported | `private:` |
| `==` on comparable structs | `bool operator==(const T&) const = default;` |
