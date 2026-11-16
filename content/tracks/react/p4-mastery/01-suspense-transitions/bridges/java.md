---
from: java
status: draft
concept: "Futures in the view, and exception barriers"
from_way: "You compose CompletableFutures, block or use callbacks to show results, and contain failures with try/catch at service boundaries."
native_way: "Components read promises with use and suspend; Suspense boundaries show placeholders; error boundaries are catch blocks for a subtree."
sensei_tip: "Think of an error boundary as a try/catch around a part of the UI tree, and a Suspense boundary as a placeholder rendered while the futures inside are not complete."
---

| Java | React |
| --- | --- |
| `future.join()` | `use(promise)` (suspends instead of blocking) |
| A cache of `CompletableFuture`s | A promise cache keyed by id |
| `try { ... } catch` around a call | Error boundary around a subtree |
| Retry with Resilience4j | Reset the boundary and evict the cached promise |
| Background priority executor | `startTransition` |
| Optimistic locking | `useOptimistic` (UI only) |
