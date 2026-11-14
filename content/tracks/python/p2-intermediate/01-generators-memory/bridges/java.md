---
from: java
status: draft
concept: "Lazy evaluation"
from_way: "Stream pipelines (`list.stream().filter().map()`) evaluated on demand."
native_way: "Functions with `yield` return a generator that keeps its state between calls and produces one item at a time."
sensei_tip: "Replace `[x for x in data]` with `(x for x in data)` when you only iterate once over a large input."
---
