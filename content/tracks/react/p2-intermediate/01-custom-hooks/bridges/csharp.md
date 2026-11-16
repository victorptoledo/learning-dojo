---
from: csharp
status: draft
concept: "Services per component instance"
from_way: "Blazor reuses logic with base components, injected scoped services, or helper classes that implement IDisposable."
native_way: "A custom hook bundles state, effects and cleanup into a function; each component that calls it gets its own copy."
sensei_tip: "A custom hook is like a transient service created for each component, disposed with it; for something shared, use context like a scoped service."
---

| C# | React |
| --- | --- |
| `ComponentBase` subclass with shared logic | A custom hook |
| Transient service per component | Each hook call has its own state |
| Scoped or singleton service | Context or a store |
| `IDisposable` on the helper | Effect cleanup inside the hook |
| `bUnit` render of a component | `renderHook` |
| Extension method | A plain function, if it uses no hooks |
