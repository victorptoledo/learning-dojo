---
from: java
status: draft
concept: "Cache-friendly Java"
from_way: "Assuming every memory access costs the same, and that objects are laid out next to each other."
native_way: "An int[] or long[] is contiguous and cache friendly; a List<Integer> is an array of pointers to boxed objects scattered on the heap; two hot fields in different threads can falsely share a line."
sensei_tip: "Prefer primitive arrays in hot loops, `@jdk.internal.vm.annotation.Contended` (or padding) for per-thread counters, and LongAdder instead of one contended AtomicLong."
---

| In Java | Underneath |
| --- | --- |
| `int[]` | contiguous: one miss per 16 ints |
| `ArrayList<Integer>` | pointers to scattered boxes |
| `int[][]` | an array of separate row arrays |
| `LongAdder` | per-cell counters to avoid one hot line |
| value classes (Valhalla) | flat layouts without object headers |
