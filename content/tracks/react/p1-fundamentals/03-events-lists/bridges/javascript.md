---
from: javascript
status: draft
concept: "addEventListener, declaratively"
from_way: "You attach listeners to nodes with addEventListener and rebuild lists by clearing and appending nodes."
native_way: "You pass handlers as props (React delegates events at the root) and return arrays of keyed elements; React moves, adds and removes DOM nodes for you."
sensei_tip: "React events are synthetic wrappers over native ones with the same API (target, preventDefault, stopPropagation); the only new habit is keys."
---

| JavaScript | React |
| --- | --- |
| `el.addEventListener("click", f)` | `onClick={f}` |
| `input.addEventListener("input", ...)` | `onChange={...}` (fires on every keystroke) |
| `e.preventDefault()` | `event.preventDefault()` |
| `ul.innerHTML = ""; items.forEach(append)` | `items.map((i) => <li key={i.id}>...)` |
| `el.hidden = !show` | `{show && <El />}` |
| `data-id` to find the item | `key` plus a closure over the item |
