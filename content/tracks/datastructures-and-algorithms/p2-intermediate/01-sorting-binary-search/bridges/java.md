---
from: java
status: draft
concept: "Sorting and searching"
from_way: "Writing comparators with subtraction, sorting a whole list to take its first element, and hand-rolling binary searches with off-by-one bugs."
native_way: "List.sort with Comparator.comparing(...).thenComparing(...) for objects (stable TimSort), Arrays.sort for primitives (dual-pivot quicksort), Collections.binarySearch / Arrays.binarySearch on sorted data, and a heap or TreeMap when the data keeps changing."
sensei_tip: "`Arrays.binarySearch` returns `-(insertionPoint) - 1` when the key is missing, so `int i = Arrays.binarySearch(a, x); if (i < 0) i = -i - 1;` gives the lower bound for distinct values. For duplicates, write your own lowerBound: the JDK does not say which equal element it finds."
---

| Need | Java |
| --- | --- |
| sort objects, stable | `list.sort(Comparator.comparing(Order::customer).thenComparing(Order::date))` |
| sort primitives | `Arrays.sort(int[])` |
| big arrays, many cores | `Arrays.parallelSort` |
| safe numeric comparator | `Comparator.comparingInt(Order::price)` / `Integer.compare` |
| search sorted data | `Arrays.binarySearch`, `Collections.binarySearch` |
| floor / ceiling in a changing set | `TreeSet.floor`, `ceiling`, `TreeMap.floorKey` |
| top k | `PriorityQueue` of size k |
