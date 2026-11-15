---
from: javascript
status: draft
concept: "Recursion and backtracking"
from_way: "Relying on tail-call optimisation because ES2015 specified it."
native_way: "Only Safari's engine implements proper tail calls; V8 (Node, Chrome) does not, and its stack allows roughly ten thousand frames. Deep recursion needs an explicit array stack, exactly as in Java. Generators (function*) are a neat way to yield backtracking results lazily."
sensei_tip: "`function* subsets(i) { ... yield [...path] ... }` lets a caller stop after the first solution with no special flag, which is the cleanest 'find one' backtracking in any of these languages."
---

| JavaScript | Java |
| --- | --- |
| `RangeError: Maximum call stack size exceeded` | `StackOverflowError` |
| no tail-call optimisation in V8 | none in the JVM either |
| `path.push(x)` / `path.pop()` | `path.add(x)` / `path.removeLast()` |
| `out.push([...path])` | `out.add(List.copyOf(path))` |
| `function*` + `yield` | an `Iterator` or a callback |
| explicit `stack = []` | `ArrayDeque` |
