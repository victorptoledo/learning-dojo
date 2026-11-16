---
from: csharp
status: draft
concept: "Resource Acquisition Is Initialization"
from_way: "The GC frees memory; IDisposable with using statements releases files, connections and locks; finalizers run late."
native_way: "Destructors release resources at scope exit; every object behaves as if it were in a using block; smart pointers own heap objects."
sensei_tip: "Dispose() and a using block map directly to a destructor and a local variable. You never need a finalizer in C++."
---

| C# | C++ |
| --- | --- |
| `using var conn = new SqlConnection(...)` | `Connection conn(...);` |
| `IDisposable.Dispose()` | destructor `~Connection()` |
| `lock (obj) { }` | `std::lock_guard lock(m);` |
| `new Widget()` (GC) | `std::make_unique<Widget>()` |
| `WeakReference<T>` | `std::weak_ptr<T>` |
| finalizer `~Widget()` (non-deterministic) | destructor `~Widget()` (deterministic) |
