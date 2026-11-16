---
from: javascript
status: draft
concept: "Virtual DOM, explained precisely"
from_way: "You may have heard that React uses a 'virtual DOM' that makes it faster than the real DOM."
native_way: "React keeps element trees and fibers in memory, diffs them with type and key heuristics, and applies minimal DOM operations in a commit; the benefit is correctness and a declarative model, not raw speed."
sensei_tip: "Hand-written DOM updates can be faster than React; React's value is that you never write them. Learn the identity rules (type, position, key) and the 'virtual DOM' stops being magic."
---

| JavaScript | React |
| --- | --- |
| `el.replaceWith(newEl)` | Different type: remount |
| `el.textContent = x` | Same type: update the changed text |
| Reorder nodes by hand | Keys: React moves the nodes |
| `requestIdleCallback` chunks | Fiber scheduler |
| Debounce heavy UI updates | `startTransition`, `useDeferredValue` |
| One update per DOM write | Batched updates per event |
