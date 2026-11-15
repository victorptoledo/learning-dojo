---
from: csharp
status: draft
concept: "Sorting and searching"
from_way: "Assuming List<T>.Sort is stable, because Java's object sort is."
native_way: "Array.Sort and List<T>.Sort use introsort (quicksort, heapsort, insertion sort) and are NOT stable; LINQ OrderBy/ThenBy is stable. Array.BinarySearch and List<T>.BinarySearch return the bitwise complement of the insertion point when absent."
sensei_tip: "If equal elements must keep their order in C#, use `OrderBy(...).ThenBy(...)` or add the original index as a last key. `~index` turns a negative BinarySearch result into the insertion point, the same idea as Java's `-(i) - 1`."
---

| C# | Java | Note |
| --- | --- | --- |
| `list.Sort(cmp)` | `list.sort(cmp)` | C# not stable, Java stable |
| `xs.OrderBy(f).ThenBy(g)` | `comparing(f).thenComparing(g)` | stable in both |
| `Array.Sort(int[])` | `Arrays.sort(int[])` | introsort / dual-pivot quicksort |
| `Array.BinarySearch(a, x)` | `Arrays.binarySearch(a, x)` | `~i` vs `-(i) - 1` when absent |
| `Comparer<T>.Create((a, b) => a.X.CompareTo(b.X))` | `Comparator.comparing(T::x)` | |
| `SortedSet<T>.GetViewBetween` | `TreeSet.subSet` | ranges |
