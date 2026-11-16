---
from: javascript
status: draft
concept: "Global objects, tamed"
from_way: "Vanilla apps often keep a global state object and an event emitter, or Redux with every API response stored in it."
native_way: "Modern React splits state by kind: query caches for server data, the URL for view state, local state by default, and a small store with selectors for shared client state."
sensei_tip: "If your Redux store is mostly API responses with isLoading flags, a query library will delete most of that code. Keep the store for what only the client knows."
---

| JavaScript | React |
| --- | --- |
| `window.appState` | A store with selectors |
| Custom event emitter | `subscribe` + `useSyncExternalStore` |
| Redux with `fetchX` thunks | TanStack Query or RTK Query |
| `location.search` | `useSearchParams` |
| Variables in a closure | Local state |
| Immer `produce` | Built into Redux Toolkit |
