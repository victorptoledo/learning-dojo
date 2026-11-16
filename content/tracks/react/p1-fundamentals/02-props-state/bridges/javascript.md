---
from: javascript
status: draft
concept: "References decide whether React re-renders"
from_way: "In plain JavaScript you mutate objects and arrays freely and update the DOM yourself afterwards."
native_way: "React compares the old and new state with Object.is; a mutated object is the same reference, so you must create new objects and arrays."
sensei_tip: "Learn the non-mutating array methods (map, filter, toSorted, toSpliced, with) and the spread operator: they are 90% of state updates."
---

| JavaScript | React |
| --- | --- |
| `let count = 0; count++` | `setCount((c) => c + 1)` |
| `arr.push(x)` | `setArr([...arr, x])` |
| `arr.sort()` | `setArr(arr.toSorted())` |
| `obj.a = 1` | `setObj({ ...obj, a: 1 })` |
| Global store object | Lift state to the common parent |
| Recompute and patch DOM by hand | Derive during render |
