---
from: python
status: draft
concept: "functools.cache for components"
from_way: "You profile with cProfile and add functools.lru_cache to pure functions that are called repeatedly with the same arguments."
native_way: "memo caches a component's output for equal props; useMemo caches a value until its dependencies change; useCallback keeps a function's identity."
sensei_tip: "lru_cache needs hashable, equal arguments; memo needs props that are the same objects. A new dict literal each call would defeat lru_cache too."
---

| Python | React |
| --- | --- |
| `cProfile`, `py-spy` | React Profiler |
| `@functools.cache` | `memo`, `useMemo` |
| Same function object | `useCallback` |
| Generators for large data | Virtualised lists |
| Lazy imports | `lazy(() => import(...))` |
| Measure before optimising | Measure before optimising |
