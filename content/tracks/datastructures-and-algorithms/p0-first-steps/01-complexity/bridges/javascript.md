---
from: javascript
status: draft
concept: "Cost of operations and amortised growth"
from_way: "Using `array.includes`, `indexOf` or `find` inside `map` and `filter` callbacks, so a pipeline that looks linear is quadratic."
native_way: "JavaScript arrays are dynamic arrays in V8 (amortised O(1) `push`), but `includes`, `indexOf`, `shift` and `unshift` are O(n). `Set` and `Map` give O(1) expected lookups."
sensei_tip: "`arr.filter(x => other.includes(x))` is O(n·m); `const s = new Set(other); arr.filter(x => s.has(x))` is O(n + m). Also beware `shift()` in a loop to use an array as a queue: each call moves every element."
---

| JavaScript | Java | Cost |
| --- | --- | --- |
| `arr.push(x)` | `list.add(x)` | amortised O(1) |
| `arr[i]` | `list.get(i)` | O(1) |
| `arr.shift()` / `arr.unshift(x)` | `list.remove(0)` / `list.add(0, x)` | O(n) |
| `arr.includes(x)` / `indexOf(x)` | `list.contains(x)` | O(n) |
| `set.has(x)` / `map.get(k)` | `set.contains(x)` / `map.get(k)` | O(1) expected |
| `arr.sort(cmp)` | `List.sort(cmp)` | O(n log n), stable since ES2019 |
