---
from: csharp
status: draft
concept: "ShouldRender, done for you"
from_way: "Blazor re-renders a component when its parameters may have changed; you override ShouldRender or use immutable parameters to skip work."
native_way: "memo plays the role of ShouldRender with a shallow comparison of props; useMemo and useCallback keep parameter instances stable."
sensei_tip: "Blazor skips re-renders for primitive parameters but not for complex ones; React is similar with memo, so stable references (useMemo, useCallback) matter the same way."
---

| Blazor / .NET | React |
| --- | --- |
| `ShouldRender()` | `memo(Component)` |
| Immutable parameter types | Stable props via `useMemo` / `useCallback` |
| `Virtualize` component | TanStack Virtual |
| dotnet-trace, browser profiler | React Profiler |
| Lazy-loaded assemblies | `lazy(() => import(...))` |
| Source generators | React Compiler |
