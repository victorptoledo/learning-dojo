---
from: python
status: draft
concept: "Functions that carry state, called in order"
from_way: "You reuse logic with functions, decorators, mixins or context managers; state lives on objects."
native_way: "A custom hook is a function that calls useState and useEffect; React stores its state by call position for each component."
sensei_tip: "Python never cared about call order; React does. Picture a list of slots per component, filled in the order hooks run: an if around a hook shifts every slot after it."
---

| Python | React |
| --- | --- |
| A mixin adding behaviour | A custom hook |
| Decorator wrapping a function | Hook composing other hooks |
| Module-level global state | Not a hook: context or a store |
| `contextlib.contextmanager` | A hook with an effect and its cleanup |
| `pytest` fixture | `renderHook` in a test |
| Naming convention `_private` | Naming convention `useSomething` |
