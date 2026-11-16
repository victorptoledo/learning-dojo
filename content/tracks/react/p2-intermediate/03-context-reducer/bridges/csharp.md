---
from: csharp
status: draft
concept: "CascadingValue and Fluxor"
from_way: "Blazor passes values down with <CascadingValue> and [CascadingParameter]; Fluxor brings reducers and actions to Blazor."
native_way: "React context is CascadingValue; useReducer is a component-local Fluxor reducer with typed actions."
sensei_tip: "If you have used CascadingValue, context is the same idea. Write reducers like C# pattern-matching switch expressions over records: action switch { Add a => ..., Remove r => ... }."
---

| Blazor / C# | React |
| --- | --- |
| `<CascadingValue Value="theme">` | `<ThemeContext value={theme}>` |
| `[CascadingParameter] Theme Theme` | `useContext(ThemeContext)` |
| Fluxor `[ReducerMethod]` | `cartReducer(state, action)` |
| `record AddItem(Item Item)` | `{ type: "add"; item: Item }` |
| `state with { Items = ... }` | `{ ...state, items: ... }` |
| Scoped service in DI | A provider placed at the right level |
