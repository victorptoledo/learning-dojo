---
from: go
status: draft
concept: "defer for a component's lifetime"
from_way: "You start a goroutine or a ticker and stop it with defer ticker.Stop() or a cancelled context."
native_way: "An effect starts the work and returns the function that stops it; React calls it when the dependencies change or the component goes away."
sensei_tip: "Treat the returned cleanup as the defer of the effect, and the dependency list as the values that would make you cancel the context and start over."
---

| Go | React |
| --- | --- |
| `t := time.NewTicker(d); defer t.Stop()` | `const id = setInterval(f, d); return () => clearInterval(id)` |
| `ctx, cancel := context.WithCancel(...)` | an `AbortController` aborted in the cleanup |
| Restart the goroutine when config changes | Dependencies `[config]` |
| A pointer to a value that is not displayed | `useRef` |
| A method computing a value | A value derived in render |
| Leaked goroutine | An effect without cleanup |
