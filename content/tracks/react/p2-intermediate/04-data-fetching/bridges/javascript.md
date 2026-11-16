---
from: javascript
status: draft
concept: "The same fetch, inside a component's lifetime"
from_way: "You call fetch on page load or on a click and write the result into the DOM; stale responses rarely matter in a static page."
native_way: "In React the component can change its inputs or disappear while a request is in flight, so you abort in the cleanup and keep loading/error/success in state."
sensei_tip: "Every fetch in an effect needs an AbortController, a response.ok check and a union state. If you write that three times, it is time for TanStack Query."
---

| JavaScript | React |
| --- | --- |
| `fetch(url).then(render)` on load | `useEffect` with fetch and `setState` |
| Show a spinner element | `status: "loading"` in state |
| `new AbortController()` | the same, aborted in the cleanup |
| Ignore late responses (often not needed) | Required: props can change |
| Manual cache in a Map | TanStack Query |
| `Promise.all` | `Promise.all`, or parallel queries |
