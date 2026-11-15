---
from: javascript
status: draft
concept: "Observer, command and chain of responsibility"
from_way: "Callbacks passed into every function, and undo implemented by deep-copying state."
native_way: "`EventTarget` and `EventEmitter` for observer, Redux-style actions (plain objects) as commands, and middleware (Express, Koa) as chain of responsibility."
sensei_tip: "A Redux action is a command as data: `{ type: 'todo/added', text }`. The reducer interprets it, and time-travel debugging is undo and redo over the list of commands."
---

| JavaScript | Pattern |
| --- | --- |
| `addEventListener` / `removeEventListener` | observer |
| `AbortController` to unsubscribe | subscription handle |
| Redux actions | command (as data) |
| `app.use(middleware)` and `next()` | chain of responsibility |
| `steps.reduce((v, f) => f(v), x)` | pipeline |
