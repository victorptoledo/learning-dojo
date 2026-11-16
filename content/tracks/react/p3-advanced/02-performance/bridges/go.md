---
from: go
status: draft
concept: "pprof first, then avoid the work"
from_way: "You run pprof, find the hot path, and remove allocations or cache results; you do not guess."
native_way: "You run the React Profiler, find the components that render too often, then colocate state, memoise components and stabilise props."
sensei_tip: "Treat the Profiler like pprof: optimise the top entries only. Most React slowness is one component owning state that half the page does not care about."
---

| Go | React |
| --- | --- |
| `pprof` | React Profiler |
| Avoid allocations in a loop | Avoid new objects passed to memoised children |
| `sync.Once` / cached result | `useMemo` |
| Compare before doing work | `memo` |
| Paginate large results | Virtualise long lists |
| Build tags to exclude code | Code splitting with `lazy` |
