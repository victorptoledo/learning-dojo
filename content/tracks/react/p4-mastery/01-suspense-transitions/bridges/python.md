---
from: python
status: draft
concept: "Awaiting inside the view"
from_way: "You await coroutines in an async view and wrap risky parts in try/except; templates render after all data is there."
native_way: "A component reads a promise with use and suspends; Suspense renders a placeholder meanwhile; an error boundary is an except block for a subtree."
sensei_tip: "use(promise) is like await, but React pauses the component instead of the function and shows the nearest fallback. Cache the coroutine's task, never create it in render."
---

| Python | React |
| --- | --- |
| `await fetch_user(id)` | `use(fetchUser(id))` |
| `functools.cache` on a task factory | A promise cache keyed by id |
| `try/except` around a block | Error boundary |
| `asyncio.gather` and show all together | One Suspense boundary for several parts |
| Low-priority background work | Transitions |
| Show "saved" before the server replies | `useOptimistic` |
