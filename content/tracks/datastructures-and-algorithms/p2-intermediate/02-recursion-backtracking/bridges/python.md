---
from: python
status: draft
concept: "Recursion and backtracking"
from_way: "Assuming Python recursion goes as deep as needed, then hitting RecursionError at depth 1,000."
native_way: "CPython limits recursion to about 1,000 frames by default (sys.getrecursionlimit), much less than Java, so deep recursion must become a loop with an explicit list as a stack. itertools.combinations, permutations and product enumerate the classic spaces without writing the backtracking."
sensei_tip: "`functools.cache` on a recursive function is memoisation in one line, turning naive Fibonacci into O(n). Raising `sys.setrecursionlimit` is risky: past the C stack size the interpreter crashes."
---

| Python | Java |
| --- | --- |
| recursion limit ~1,000 | stack of ~10,000s of frames (`-Xss`) |
| `itertools.combinations(xs, k)` | backtracking with a `start` index |
| `itertools.permutations(xs)` | backtracking with `boolean[] used` |
| `itertools.product(xs, repeat=n)` | n nested choices |
| `path.append(x)` / `path.pop()` | `path.add(x)` / `path.removeLast()` |
| `@functools.cache` | a `HashMap` memo |
