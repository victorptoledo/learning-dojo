---
from: javascript
status: draft
concept: "Plain functions, plus the rules"
from_way: "You extract repeated code into helper functions and modules; any function can be called anywhere, in any order."
native_way: "Custom hooks are helper functions that call hooks; they must be called unconditionally at the top level of components and other hooks."
sensei_tip: "Extract a hook exactly as you would extract a function, then check two things: its name starts with use, and every caller calls it at the top level."
---

| JavaScript | React |
| --- | --- |
| `function getWidth()` | `function useWindowWidth()` (with state and an effect) |
| A module singleton | Not a hook: each call is separate |
| Call anywhere | Call only at the top level, never conditionally |
| `return { data, error }` | Same: return an object or a tuple |
| Closures over variables | Hooks re-run with fresh props on every render |
| Unit test the function | `renderHook` + `act` |
