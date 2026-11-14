---
from: java
status: draft
concept: "Data access and cache misses"
from_way: "Assuming every variable access in memory costs the same."
native_way: "An L1 hit takes about 1 ns, RAM about 100 ns, a disk read millions of ns. Contiguous arrays make use of CPU cache lines."
sensei_tip: "Data-oriented design: iterating over contiguous structs can be many times faster than chasing pointers to objects scattered on the heap."
---
