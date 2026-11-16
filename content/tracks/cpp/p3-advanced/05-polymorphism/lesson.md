# Polymorphism: Virtual Functions, std::variant and Type Erasure

C++ has several ways to let one piece of code work with values of different types. Each
answers a different question: **is the set of types open or closed?** and **do you want to
pay at run time or at compile time?** Knowing all four is what lets you pick the simplest one.

| Technique | Set of types | Dispatch | Typical use |
| --- | --- | --- | --- |
| virtual functions | open (anyone can derive) | run time, through a vtable | plugins, frameworks, stable interfaces |
| `std::variant` + `std::visit` | closed (listed in the type) | run time, by index, no heap | messages, AST nodes, states |
| templates and concepts | open | compile time | algorithms, containers |
| type erasure (`std::function`, `std::any`, hand-written) | open | run time, value semantics | callbacks, "any shape" values |

## Virtual functions

```cpp
class Shape {
public:
    virtual ~Shape() = default;                  // a virtual destructor: always, in a base
    virtual double area() const = 0;             // pure virtual: Shape is abstract
    virtual std::string name() const { return "shape"; }
};

class Circle final : public Shape {
public:
    explicit Circle(double r) : r_(r) {}
    double area() const override { return std::numbers::pi * r_ * r_; }
    std::string name() const override { return "circle"; }
private:
    double r_;
};

std::vector<std::unique_ptr<Shape>> shapes;
shapes.push_back(std::make_unique<Circle>(1.0));
for (const auto& s : shapes) { std::println("{} {}", s->name(), s->area()); }
```

- A call through a base pointer or reference goes through the object's **vtable** (one
  indirect call), so the derived override runs.
- **Virtual destructor**: deleting a derived object through a base pointer without one is
  undefined behaviour (the derived part is never destroyed).
- Write `override` on every override (the compiler checks it), and `final` on classes or
  functions that must not be overridden further (it also helps the optimiser).
- **Slicing**: copying a derived object into a base *value* (`Shape s = circle;` or
  `std::vector<Shape>`) keeps only the base part. Polymorphic objects are handled through
  pointers or references, and usually owned by `std::unique_ptr`.
- Do not call virtual functions from constructors or destructors: the derived part does not
  exist yet (or any more).

Keep hierarchies shallow and interfaces small. "Composition over inheritance" applies to C++
too; inheritance is for "is-a" with a stable interface, not for code reuse.

## std::variant: a closed set of alternatives

```cpp
#include <variant>

struct Deposit { std::int64_t cents; };
struct Withdrawal { std::int64_t cents; };
struct Transfer { std::string to; std::int64_t cents; };

using Event = std::variant<Deposit, Withdrawal, Transfer>;

template <typename... Fs>
struct overloaded : Fs... { using Fs::operator()...; };   // a classic helper

std::int64_t delta(const Event& e) {
    return std::visit(overloaded{
        [](const Deposit& d) { return d.cents; },
        [](const Withdrawal& w) { return -w.cents; },
        [](const Transfer& t) { return -t.cents; },
    }, e);
}
```

A variant stores one of its alternatives inline (its size is the largest alternative plus an
index), so there is no heap allocation and no common base. `std::visit` fails to compile if a
case is missing, which is the C++ form of exhaustive pattern matching. Use
`std::holds_alternative<T>(v)` and `std::get_if<T>(&v)` for one case.

Variants fit when you know every alternative up front and operations change more often than
types; virtual functions fit when types change more often than operations.

## Type erasure: value semantics for open sets

`std::function<double(double)>` stores any callable with that signature: a lambda, a function
pointer, a functor. That is **type erasure**: a concrete type that hides the real type behind
an internal interface, while behaving like a value (copyable, no pointers for the user).

```cpp
std::function<void(const Event&)> on_event = [&log](const Event& e) { log.push_back(e); };
std::move_only_function<void()> task = [p = std::make_unique<Job>()] { p->run(); };   // C++23
```

The same pattern lets you write a `Shape` type that holds *any* type with an `area()`
function, without inheritance in user code (Sean Parent's "runtime polymorphism"): a small
internal base class with a virtual function, a templated model that wraps the user's type,
and a `unique_ptr` to it inside a value type. `std::any` is the extreme version: any copyable
type, with checked casts back.

## Static polymorphism

Templates and concepts dispatch at compile time, with no indirection and full inlining:

```cpp
template <typename T>
concept HasArea = requires(const T& s) { { s.area() } -> std::convertible_to<double>; };

double total_area(const std::vector<Circle>& circles);       // concrete
template <HasArea S> double total_area(std::span<const S> shapes);   // generic, no vtable
```

The **CRTP** (curiously recurring template pattern, `class Derived : Base<Derived>`) and C++23
**deducing this** (`void draw(this const auto& self)`) reuse code across types without virtual
calls.

## Summary

- Virtual functions for open hierarchies: virtual destructor, `override`, no slicing.
- `std::variant` + `std::visit` for a closed set of types with exhaustive handling, no heap.
- Type erasure (`std::function`, `std::move_only_function`, hand-written) for open sets with
  value semantics.
- Templates and concepts when the type is known at compile time.

## Further reading

- cppreference, virtual function specifier: <https://en.cppreference.com/w/cpp/language/virtual>
- cppreference, std::variant and std::visit: <https://en.cppreference.com/w/cpp/utility/variant>
- cppreference, std::move_only_function: <https://en.cppreference.com/w/cpp/utility/functional/move_only_function>
- C++ Core Guidelines, C.hier: Class hierarchies (C.35, C.67, C.128): <https://isocpp.github.io/CppCoreGuidelines/CppCoreGuidelines#SS-hier>
- Sean Parent, "Inheritance Is The Base Class of Evil" (GoingNative 2013)
- Klaus Iglberger, *C++ Software Design* (O'Reilly, 2022), on type erasure and variants
