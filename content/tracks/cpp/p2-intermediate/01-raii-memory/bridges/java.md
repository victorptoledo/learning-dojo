---
from: java
status: draft
concept: "Resource Acquisition Is Initialization"
from_way: "Relying on try-with-resources or a garbage collector to clean up resources."
native_way: "When a C++ object goes out of scope its destructor `~ClassName()` runs immediately, releasing files and memory."
sensei_tip: "Avoid manual `new` and `delete` in modern C++: use `std::make_unique` and references."
---
