---
from: python
status: draft
concept: "Resource Acquisition Is Initialization"
from_way: "Reference counting plus a cycle collector free objects; with statements and context managers close files and release locks."
native_way: "Every object is destroyed at a defined point; destructors play the role of __exit__ for every resource, without a with block."
sensei_tip: "A C++ local object is a context manager that is always active: its destructor is your __exit__, and it also runs when an exception passes."
---

| Python | C++ |
| --- | --- |
| `with open(p) as f:` | `std::ifstream f(p);` (closed at end of scope) |
| `with lock:` | `std::lock_guard lock(m);` |
| `__del__` (unpredictable) | destructor `~T()` (deterministic) |
| every name is a counted reference | `std::unique_ptr` (one owner) or `std::shared_ptr` (counted) |
| `weakref.ref(obj)` | `std::weak_ptr` |
| cycles freed by the GC | `shared_ptr` cycles leak: break them with `weak_ptr` |
