---
from: javascript
status: draft
concept: "Classes and objects"
from_way: "class syntax with a constructor, #private fields, getters and setters, and objects always by reference."
native_way: "Classes with typed members, access control, const member functions and value semantics; comparisons can be generated."
sensei_tip: "TypeScript's `readonly` field is closest to a const member; C++ adds const member functions, which say a method does not change the object."
---

| JavaScript / TypeScript | C++ |
| --- | --- |
| `class Account { #balance = 0 }` | `class Account { std::int64_t balance_ = 0; };` |
| `constructor(owner) { this.owner = owner }` | `explicit Account(std::string owner) : owner_(std::move(owner)) {}` |
| `get balance()` | `std::int64_t balance() const` |
| `readonly` (TS) | `const` member function / `const` member |
| `a === b` (identity) | `a == b` (value comparison) |
| objects by reference | objects are values; `&` for references |
