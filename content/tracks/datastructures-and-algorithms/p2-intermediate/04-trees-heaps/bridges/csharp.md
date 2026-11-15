---
from: csharp
status: draft
concept: "Trees and heaps"
from_way: "Using SortedList<K,V> as a sorted map without knowing its inserts are O(n)."
native_way: "SortedDictionary<K,V> and SortedSet<T> are red-black trees (O(log n)), like Java's TreeMap and TreeSet; SortedList<K,V> is two sorted arrays. PriorityQueue<TElement, TPriority> (since .NET 6) is a quaternary (4-ary) heap, with the priority stored separately from the element."
sensei_tip: "`SortedSet<T>.GetViewBetween(lo, hi)` is Java's subSet, and `PriorityQueue.EnqueueDequeue` pushes and pops in one step, which is exactly the top-k replacement move."
---

| C# | Java | Note |
| --- | --- | --- |
| `PriorityQueue<TElement, TPriority>` | `PriorityQueue<E>` | 4-ary vs binary heap |
| `SortedDictionary<K,V>` | `TreeMap<K,V>` | red-black tree |
| `SortedSet<T>` | `TreeSet<T>` | red-black tree |
| `SortedList<K,V>` | (sorted `ArrayList`) | O(n) inserts |
| `set.GetViewBetween(a, b)` | `subSet(a, true, b, true)` | range view |
| `set.Min` / `set.Max` | `first()` / `last()` | O(log n) |
