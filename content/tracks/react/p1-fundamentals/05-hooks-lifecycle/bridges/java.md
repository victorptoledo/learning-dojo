---
from: java
status: draft
concept: "Resources with a scope, like try-with-resources"
from_way: "You open a resource and close it in finally or with try-with-resources; listeners added with addListener must be removed to avoid leaks."
native_way: "An effect opens a resource while the component shows it and returns a cleanup that closes it; React calls the cleanup on re-sync and unmount."
sensei_tip: "Read each useEffect as a try-with-resources block: the body is the try, the returned function is close(). If there is nothing to close, ask whether you need the effect."
---

| Java | React |
| --- | --- |
| `try (var conn = open(room)) { ... }` | `useEffect(() => { const c = open(room); return () => c.close(); }, [room])` |
| `addListener` / `removeListener` | subscribe in the effect, unsubscribe in the cleanup |
| `ScheduledExecutorService` + `cancel` | `setInterval` + `clearInterval` in cleanup |
| A field that does not affect the view | `useRef` |
| `@PostConstruct` / `@PreDestroy` | Not quite: effects re-run when dependencies change |
| A getter computing a value | A value derived in render, no effect |
