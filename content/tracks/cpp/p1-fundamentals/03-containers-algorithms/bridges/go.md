---
from: go
status: draft
concept: "Slices, maps and sorting"
from_way: "Slices share backing arrays, maps have random iteration order, slices.SortFunc and maps.Keys from the standard library."
native_way: "Vectors own their elements and copy deeply; std::map is sorted and std::unordered_map unordered; the algorithms library has sorting, searching and more."
sensei_tip: "A Go slice header is cheap to copy; a std::vector copy duplicates every element. Pass `std::span` or `const std::vector<T>&` instead."
---

| Go | C++ |
| --- | --- |
| `[]int` (shares) | `std::vector<int>` (owns) / `std::span<int>` (shares) |
| `map[string]int` | `std::unordered_map<std::string, int>` |
| sorted iteration over keys | `std::map` iterates in key order |
| `v, ok := m[k]` | `m.find(k) != m.end()` |
| `slices.SortFunc(s, cmp)` | `std::ranges::sort(v, less)` (bool, not int) |
| `append` may reallocate | `push_back` may reallocate and invalidate iterators |
