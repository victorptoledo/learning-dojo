---
from: csharp
status: draft
concept: "IDisposable for a component's subscriptions"
from_way: "Blazor has OnInitialized, OnParametersSet and IDisposable.Dispose; WPF subscribes to events and must unsubscribe to avoid leaks."
native_way: "One useEffect per concern: subscribe in the body, unsubscribe in the returned cleanup, and list the parameters it depends on."
sensei_tip: "An effect is a using block whose Dispose React calls for you, either when the dependencies change or when the component leaves the screen."
---

| C# | React |
| --- | --- |
| `OnParametersSet` + `Dispose` | `useEffect(..., [param])` with cleanup |
| `event += handler` / `event -= handler` | subscribe / unsubscribe in the cleanup |
| `Timer` + `Dispose()` | `setInterval` + `clearInterval` |
| `ElementReference` with `@ref` | `useRef<HTMLInputElement>(null)` |
| A private field not shown in markup | `useRef` |
| Computed property | Derived value in render |
