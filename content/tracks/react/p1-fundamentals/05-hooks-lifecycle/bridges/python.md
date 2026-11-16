---
from: python
status: draft
concept: "Context managers for the lifetime of a component"
from_way: "with blocks open and close resources: `with connect(room) as conn:`; callbacks registered must be unregistered."
native_way: "useEffect opens the resource and returns the function that closes it; the dependency list says when to close and reopen."
sensei_tip: "An effect is __enter__ and its cleanup is __exit__, re-entered whenever a dependency changes. Write the __exit__ first."
---

| Python | React |
| --- | --- |
| `with connect(room) as c:` | `useEffect(() => { const c = connect(room); return () => c.close(); }, [room])` |
| `signal.connect(f)` / `disconnect(f)` | add in the effect, remove in the cleanup |
| `threading.Timer(...).cancel()` | `setTimeout` + `clearTimeout` |
| An attribute that is not displayed | `useRef` |
| `@functools.cached_property` | derive in render (`useMemo` if slow) |
| `nonlocal` mutable box | `ref.current` |
