---
from: csharp
status: draft
concept: "Moving instead of copying"
from_way: "Classes are references; structs are copied; there is no move, only copies of structs or shared references."
native_way: "Every type can be moved: the move constructor steals the internal buffers, which makes big value types cheap to pass and return."
sensei_tip: "Large C# structs are copied on every pass; in C++ you would make them movable (rule of zero gives that for free) and std::move them."
---

| C# | C++ |
| --- | --- |
| struct copy on assignment | copy, or move with `std::move` |
| class reference sharing | `std::shared_ptr`, `T&`, or move to transfer |
| `ref` return | return by value (moved or elided) |
| no move constructor | `T(T&&) noexcept` |
| readonly struct | `const T` |
