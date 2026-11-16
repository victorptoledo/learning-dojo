---
from: go
status: draft
concept: "Resource Acquisition Is Initialization"
from_way: "The GC frees memory; defer releases files, locks and connections at the end of the function."
native_way: "Destructors release resources at the end of each scope automatically, without writing defer, and also when an exception unwinds."
sensei_tip: "Every `defer f.Close()` you write in Go is a destructor you do not have to write in C++: the object closes itself."
---

| Go | C++ |
| --- | --- |
| `f, err := os.Open(p); defer f.Close()` | `std::ifstream f(p);` |
| `mu.Lock(); defer mu.Unlock()` | `std::lock_guard lock(mu);` |
| GC frees memory | the owner's destructor frees memory |
| `&T{}` escapes to the heap | `std::make_unique<T>()` (explicit heap) |
| runtime.SetFinalizer | destructor (deterministic) |
| defer runs at function end | destructors run at block end |
