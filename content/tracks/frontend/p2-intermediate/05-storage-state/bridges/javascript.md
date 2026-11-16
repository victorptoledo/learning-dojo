---
from: javascript
status: draft
concept: "State without a framework store"
from_way: "Redux, Zustand or framework context holding app state, maybe persisted by a plugin."
native_way: "The platform's own places: memory, the URL, Web Storage and IndexedDB; a store is just organized memory."
sensei_tip: "Before adding a store, ask whether the state belongs in the URL (shareable) or on the server (cached with a query library); what remains is often small."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| Redux store | Memory: a module-level object |
| `redux-persist` | `localStorage` with versioned JSON |
| React Router search params | `URLSearchParams` + `history.replaceState` |
| TanStack Query cache | Server state with refetching |
