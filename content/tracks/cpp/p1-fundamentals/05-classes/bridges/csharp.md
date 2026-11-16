---
from: csharp
status: draft
concept: "Classes and structs"
from_way: "class (reference) and struct (value), properties, readonly members, records with value equality."
native_way: "C++ classes are all value types; const member functions replace readonly; = default comparisons replace record equality."
sensei_tip: "A C# record struct is the closest thing to a C++ struct with `auto operator<=>(const T&) const = default;`."
---

| C# | C++ |
| --- | --- |
| `class Account` (reference) | `class Account` (value) |
| `public long Balance { get; private set; }` | `std::int64_t balance() const` + private member |
| `readonly` method/struct | `const` member function |
| `record Point(int X, int Y)` | `struct Point` + defaulted `<=>` |
| `IEquatable<T>` | `operator==` |
| `IDisposable` | destructor (see RAII) |
