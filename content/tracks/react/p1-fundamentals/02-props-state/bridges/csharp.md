---
from: csharp
status: draft
concept: "Records with with-expressions"
from_way: "Blazor fields change and StateHasChanged re-renders; WPF view models raise PropertyChanged; collections are often mutable ObservableCollection."
native_way: "useState holds an immutable value; the setter replaces it and re-renders. Copies with spread are React's with-expressions."
sensei_tip: "Treat state like a C# record: `user with { Email = x }` is exactly `{ ...user, email: x }`, and the new instance goes to the setter."
---

| C# | React |
| --- | --- |
| `private int count;` + `StateHasChanged()` | `const [count, setCount] = useState(0)` |
| `user with { Email = x }` | `{ ...user, email: x }` |
| `list.Add(x)` | `setList([...list, x])` |
| `ImmutableList.Remove` | `list.filter(...)` |
| `[Parameter]` from parent + `EventCallback` | Props down, callbacks up |
| Computed property `=> First + Last` | Derived value in render |
