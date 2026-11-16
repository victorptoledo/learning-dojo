---
from: go
status: draft
concept: "Composable functions with hidden per-caller state"
from_way: "You reuse code with plain functions and small types with methods; state lives in values you pass around explicitly."
native_way: "A custom hook is a function whose state React keeps per component, matched by the order in which hooks are called."
sensei_tip: "In Go state is explicit; in hooks it is implicit, stored by call order. Keep hooks at the top of the function, like declarations, and the magic stays predictable."
---

| Go | React |
| --- | --- |
| A constructor `NewTracker()` per caller | Each `useTracker()` call has its own state |
| Package-level variable | Not a hook: context or a store |
| `defer cleanup()` | Effect cleanup inside the hook |
| Interface for the API | Typed parameters and return value |
| Table-driven test | `renderHook` with several `rerender` calls |
| Naming `NewX` | Naming `useX` |
