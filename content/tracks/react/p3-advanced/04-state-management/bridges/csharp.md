---
from: csharp
status: draft
concept: "Fluxor stores and HttpClient caches"
from_way: "Blazor apps use cascading values, scoped services as state containers, or Fluxor (Redux for .NET); HTTP responses are cached with IMemoryCache."
native_way: "React uses context for cascading values, Zustand or Redux Toolkit for state containers, and TanStack Query as the HTTP response cache."
sensei_tip: "A scoped 'state container' service with an OnChange event is a Zustand store; the difference is that Zustand selectors decide who re-renders."
---

| Blazor / .NET | React |
| --- | --- |
| Scoped state container service | Zustand store |
| Fluxor | Redux Toolkit |
| `IMemoryCache` for API data | TanStack Query |
| `CascadingValue` | Context |
| `NavigationManager` query | Search params |
| Component fields | Local state |
