---
from: csharp
status: draft
concept: "ErrorBoundary and streaming rendering in Blazor"
from_way: "Blazor has an <ErrorBoundary> component with Recover(), and streaming rendering shows placeholders while async OnInitializedAsync completes."
native_way: "React's error boundaries and Suspense do the same per subtree; use reads a Task-like promise; transitions keep old content during updates."
sensei_tip: "Blazor's <ErrorBoundary> with Recover() maps to an error boundary with a reset; [StreamRendering] placeholders map to Suspense fallbacks."
---

| Blazor / C# | React |
| --- | --- |
| `<ErrorBoundary>` + `Recover()` | Error boundary + reset |
| `[StreamRendering]` placeholders | `<Suspense fallback>` |
| `await` in `OnInitializedAsync` | `use(promise)` |
| `Lazy<Task<T>>` cache | Promise cache keyed by id |
| Low-priority dispatch | `startTransition` |
| Optimistic UI by hand | `useOptimistic` |
