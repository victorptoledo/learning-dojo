---
from: javascript
status: draft
concept: "Polymorphism"
from_way: "Prototype-based dispatch, classes with extends, TypeScript discriminated unions narrowed with switch on a kind field."
native_way: "Virtual functions for hierarchies, std::variant for discriminated unions (the index is the kind), std::function for callbacks."
sensei_tip: "A TypeScript discriminated union `{ kind: 'a' } | { kind: 'b' }` is a std::variant, and an exhaustive switch is std::visit."
---

| JavaScript / TypeScript | C++ |
| --- | --- |
| `class Circle extends Shape` | `class Circle final : public Shape` |
| method overriding | `virtual` + `override` |
| `type Event = A \| B` with `kind` | `std::variant<A, B>` |
| exhaustive `switch (e.kind)` | `std::visit(overloaded{...}, e)` |
| callbacks `(x) => ...` | `std::function<void(X)>` |
| `instanceof` | `dynamic_cast` (avoid), `std::holds_alternative` |
