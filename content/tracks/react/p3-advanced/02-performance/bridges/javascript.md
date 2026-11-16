---
from: javascript
status: draft
concept: "Reference equality is the whole game"
from_way: "You optimise DOM updates by hand: batch writes, debounce input, cache computed results in variables."
native_way: "React re-renders by default; memo skips renders when props are reference-equal, and useMemo and useCallback preserve references between renders."
sensei_tip: "Before memoising, check whether moving state down or passing children solves it. It usually does, and it never goes stale."
---

| JavaScript | React |
| --- | --- |
| Chrome Performance panel | Plus the React Profiler |
| Cache in a closure variable | `useMemo` |
| Keep one handler reference | `useCallback` |
| `===` on objects | memo's shallow comparison |
| Debounce input handlers | `useDeferredValue`, transitions |
| Dynamic `import()` | `lazy` + `Suspense` |
