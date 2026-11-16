---
from: python
status: draft
concept: "contextvars for a component tree, and a dispatch table"
from_way: "Flask's g or contextvars give code access to request-wide values; state machines are written as functions over a state and an event."
native_way: "Context provides a value to every component below it; useReducer applies typed events to state through one pure function."
sensei_tip: "Write the reducer like a pure state-machine function in Python: match event.type, return a new state, never mutate. Then context makes state and dispatch available to the tree."
---

| Python | React |
| --- | --- |
| `contextvars.ContextVar("theme")` | `createContext<Theme>("light")` |
| `var.set(value)` within a scope | `<ThemeContext value={...}>` around a subtree |
| `var.get()` | `useContext(ThemeContext)` |
| `match event: case Add(item): ...` | `switch (action.type) { case "add": ... }` |
| Return a new frozen dataclass | Return a new state object |
| `LookupError` when unset | The guard hook throws outside the provider |
