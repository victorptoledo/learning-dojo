---
from: csharp
status: draft
concept: "EventCallback and @key"
from_way: "Blazor uses @onclick, EventCallback parameters and @foreach with @key; WPF uses ItemsControl with templates."
native_way: "React uses onClick with a function, callback props for child events, and map with key for lists."
sensei_tip: "Blazor's @key is React's key, with the same advice: use the item's id so the diff keeps the right component state for each item."
---

| Blazor / C# | React |
| --- | --- |
| `@onclick="Save"` | `onClick={save}` |
| `[Parameter] EventCallback OnDelete` | `onDelete: () => void` prop |
| `@foreach (var t in todos)` | `todos.map((t) => ...)` |
| `@key="t.Id"` | `key={t.id}` |
| `@if (user is not null)` | `{user && ...}` or early return |
| `items.Where(...).Select(...)` | `items.filter(...).map(...)` |
