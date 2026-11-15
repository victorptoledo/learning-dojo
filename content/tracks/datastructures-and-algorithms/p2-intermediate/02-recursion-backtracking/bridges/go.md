---
from: go
status: draft
concept: "Recursion and backtracking"
from_way: "Avoiding recursion for fear of stack overflows, as in Java."
native_way: "Goroutine stacks start small and grow dynamically up to a large limit (1 GB on 64-bit by default), so recursion a million deep usually just works in Go, at the cost of memory. Backtracking uses slices for the path: append to choose, re-slice to undo."
sensei_tip: "Beware of aliasing when recording results: `out = append(out, path)` stores a slice that the next append may overwrite. Copy it: `out = append(out, slices.Clone(path))`."
---

| Go | Java |
| --- | --- |
| growable goroutine stacks | fixed thread stack (`-Xss`) |
| `path = append(path, x)` / `path = path[:len(path)-1]` | `path.add(x)` / `path.removeLast()` |
| `out = append(out, slices.Clone(path))` | `out.add(List.copyOf(path))` |
| `used := make([]bool, n)` | `boolean[] used` |
| goroutines + `sync.WaitGroup` | `ForkJoinPool` |
