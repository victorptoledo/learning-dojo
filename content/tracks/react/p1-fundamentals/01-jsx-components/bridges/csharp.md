---
from: csharp
status: draft
concept: "Razor components without the markup file"
from_way: "Blazor components are .razor files with markup, @code blocks and [Parameter] properties; ChildContent is a RenderFragment."
native_way: "React components are functions in .tsx files; props replace [Parameter] properties and children replaces ChildContent."
sensei_tip: "Map Blazor one to one: [Parameter] is a prop, ChildContent is children, @if and @foreach become ternaries and map. The big change is that the whole component is one function call."
---

| Blazor | React |
| --- | --- |
| `Avatar.razor` | `Avatar.tsx` with `function Avatar(...)` |
| `[Parameter] public string Name { get; set; }` | `type AvatarProps = { name: string }` |
| `RenderFragment ChildContent` | `children: ReactNode` |
| `@Name` | `{name}` |
| `@if (ok) { <X /> }` | `{ok && <X />}` |
| `class="title"` | `className="title"` |
