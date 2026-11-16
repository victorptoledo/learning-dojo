---
from: java
status: draft
concept: "Profile, then cache"
from_way: "You profile with JFR or a profiler, then cache expensive results (memoisation, @Cacheable) and avoid allocating in hot loops."
native_way: "You profile with the React Profiler, then skip renders with memo and keep values stable with useMemo and useCallback; the React Compiler does it for you."
sensei_tip: "memo is equals() on props: it only helps if the props implement value equality, and in JavaScript a new lambda or array never equals the old one. useCallback and useMemo give you the same instance back."
---

| Java | React |
| --- | --- |
| JFR / async-profiler | React DevTools Profiler |
| Memoise a pure function | `useMemo(() => compute(x), [x])` |
| Reuse the same lambda instance | `useCallback(fn, deps)` |
| `equals` check before work | `memo(Component)` |
| JIT optimisations | React Compiler |
| Lazy class loading | `lazy(() => import(...))` |
