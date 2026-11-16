---
from: javascript
status: draft
concept: "Declarative loading and error states"
from_way: "You track isLoading and error flags by hand for every request and show spinners imperatively."
native_way: "Suspense and error boundaries declare loading and error UI per region; components read data with use as if it were already there."
sensei_tip: "Suspense moves the isLoading checks out of your components into boundaries. The price is that promises must be cached, which is why libraries and frameworks provide them."
---

| JavaScript | React |
| --- | --- |
| `if (isLoading) showSpinner()` | `<Suspense fallback={<Spinner />}>` |
| `.catch(showError)` | Error boundary fallback |
| `await promise` | `use(promise)` |
| Memoised promise in a Map | Same, required for use |
| `requestIdleCallback` for heavy UI | `startTransition`, `useDeferredValue` |
| Update UI before the response | `useOptimistic` |
