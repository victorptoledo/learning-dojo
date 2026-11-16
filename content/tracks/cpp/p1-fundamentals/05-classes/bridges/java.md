---
from: java
status: draft
concept: "Classes and objects"
from_way: "Classes with private fields, constructors, getters and setters, equals/hashCode, and objects always on the heap through references."
native_way: "Classes with private members and invariants, member initialiser lists, const member functions, objects that are values, and defaulted comparisons."
sensei_tip: "Do not write getters and setters by reflex: expose operations that keep the invariant, and mark every read-only member function const."
---

| Java | C++ |
| --- | --- |
| `private long balance;` | `private: std::int64_t balance_ = 0;` |
| constructor body assigns fields | member initialiser list `: balance_(b)` |
| `record Point(int x, int y)` | `struct Point { int x; int y; auto operator<=>(const Point&) const = default; };` |
| `equals` / `compareTo` | `operator==` / `operator<=>` (can be `= default`) |
| no `const` methods | `int balance() const` |
| `new Account(...)` | `Account a{...};` (on the stack) |
| `toString()` | `std::formatter<T>` or `operator<<` |
