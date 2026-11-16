---
from: python
status: draft
concept: "Performance"
from_way: "Interpreted code, so hot loops move to NumPy or C extensions; cProfile and py-spy find hot spots."
native_way: "Compiled code with control over memory; loops are fast already, and the work is layout, allocations and algorithms; perf finds hot spots."
sensei_tip: "Python makes you avoid loops; C++ makes loops fast, so your attention moves to memory access patterns."
---

| Python | C++ |
| --- | --- |
| cProfile, py-spy | perf, VTune |
| timeit | Google Benchmark |
| NumPy arrays (contiguous) | `std::vector<double>` (contiguous) |
| list of objects (pointers) | vector of structs (values) |
| `__slots__` to save memory | member ordering, smaller types |
| C extension for speed | already native |
