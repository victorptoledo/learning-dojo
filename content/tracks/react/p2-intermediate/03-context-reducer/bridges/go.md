---
from: go
status: draft
concept: "context.Context values and event loops"
from_way: "context.WithValue carries request-scoped values down the call tree; state machines switch on event types and return the next state."
native_way: "React context carries a value down the component tree; a reducer switches on action.type and returns the next state."
sensei_tip: "React context is context.WithValue for components, with one improvement: it is typed. Keep it for what context.Value is for in Go, cross-cutting values, not all your state."
---

| Go | React |
| --- | --- |
| `ctx = context.WithValue(ctx, key, user)` | `<SessionContext value={session}>` |
| `ctx.Value(key).(User)` | `useSession()` (typed, guarded) |
| `switch ev := e.(type)` | `switch (action.type)` |
| Return a new struct value | Return a new state object |
| Channel of events to a loop | `dispatch(action)` |
| Panic on missing value | The guard hook throws |
