---
from: java
status: draft
concept: "Data structures and pointers"
from_way: "Growable lists and hash maps where everything is an object behind a heap reference."
native_way: "Slices are windows over contiguous arrays. Go has pointers (`*T`) but no pointer arithmetic."
sensei_tip: "Go lets you pass a struct by value (a copy) or by pointer (`*User`), which influences stack vs heap allocation."
---
