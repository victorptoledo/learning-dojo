---
from: java
status: draft
concept: "Values and references"
from_way: "Primitives are values but every object is a reference to the heap; == compares references; strings are immutable."
native_way: "Every variable is the object itself: assignment and parameters copy unless you use a reference (&) or a pointer; std::string is mutable and copied deeply."
sensei_tip: "When you write `Foo b = a;` in C++ you get a second Foo, not a second name for the first one. Reach for `const Foo&` when you only want to look."
---

| Java | C++ |
| --- | --- |
| `int`, `long` with fixed sizes | `int`, `long long` (minimum sizes); `std::int32_t` for exact widths |
| `List<Integer> b = a;` (same list) | `std::vector<int> b = a;` (a copy) |
| `final int x` | `const int x` / `constexpr int x` |
| integer overflow wraps | signed overflow is undefined behaviour |
| `String` (immutable) | `std::string` (mutable, value) |
| `substring` view? no, copies | `std::string_view` (borrowed, no copy) |
| `null` references | references cannot be null; pointers can (`nullptr`) |
