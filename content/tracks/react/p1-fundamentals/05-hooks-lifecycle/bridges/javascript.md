---
from: javascript
status: draft
concept: "addEventListener with a matching removeEventListener"
from_way: "You attach listeners, start intervals and open sockets when a page loads, and often never clean them up because the page reloads anyway."
native_way: "In a single-page app components come and go; each effect returns a cleanup that undoes what it started, and dependencies decide when it restarts."
sensei_tip: "For every addEventListener, setInterval or new WebSocket in an effect, write the removeEventListener, clearInterval or close() in the returned function before anything else."
---

| JavaScript | React |
| --- | --- |
| `window.addEventListener("resize", f)` on load | `useEffect(() => { add; return remove; }, [])` |
| `setInterval(tick, 1000)` | the same in an effect, `clearInterval` in cleanup |
| `document.getElementById("q").focus()` | `inputRef.current?.focus()` |
| A module variable for a timer id | `useRef<number \| null>(null)` |
| Watch a variable and recompute | Derive in render |
| `DOMContentLoaded` | Not an equivalent: effects re-sync on changes |
