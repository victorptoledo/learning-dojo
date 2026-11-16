---
from: go
status: draft
concept: "Values, not shared pointers"
from_way: "Go copies structs by value but slices share their backing array, so append and index assignment can surprise you; state changes are explicit assignments."
native_way: "React state must be replaced with a new value: copy the object or array, change the copy, and call the setter."
sensei_tip: "Think of a slice that shares its backing array: mutating it changes what others see. React wants a fresh slice every time, like slices.Clone before changing it."
---

| Go | React |
| --- | --- |
| `s.Count++` | `setCount((c) => c + 1)` |
| `items = append(items, x)` | `setItems([...items, x])` |
| `slices.Clone(items)` then sort | `items.toSorted(...)` |
| `u2 := u; u2.Email = x` | `{ ...u, email: x }` |
| `slices.DeleteFunc` | `items.filter(...)` |
| A method computing a value | A value derived during render |
