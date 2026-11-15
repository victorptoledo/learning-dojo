---
from: python
status: draft
concept: "Dynamic programming"
from_way: "Writing memo dicts by hand and passing them through every call."
native_way: "@functools.cache (or lru_cache(None)) memoises any function of hashable arguments in one line, so top-down DP in Python is the brute force plus a decorator. Big inputs still need bottom-up loops because of the ~1,000-frame recursion limit."
sensei_tip: "Call `f.cache_clear()` between independent test cases, or the cache grows forever. Python ints never overflow, so counting DPs do not need a modulus unless the problem asks for one."
---

| Python | Java |
| --- | --- |
| `@functools.cache` | `HashMap` / array memo |
| `dp = [0] * (n + 1)` | `long[] dp = new long[n + 1]` |
| `dp = [[0] * (m + 1) for _ in range(n + 1)]` | `int[][] dp` |
| `math.inf` | `Integer.MAX_VALUE` marker |
| `bisect.bisect_left(tails, x)` | lowerBound for LIS |
| unbounded ints | `long` or `BigInteger` |
