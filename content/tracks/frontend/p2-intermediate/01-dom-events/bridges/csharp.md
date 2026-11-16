---
from: csharp
status: draft
concept: "Events and routed events"
from_way: "WPF's routed events: tunneling (Preview*) and bubbling, with e.Handled."
native_way: "DOM events capture (tunnel) and bubble the same way; stopPropagation is e.Handled = true."
sensei_tip: "If you know WPF's PreviewMouseDown and MouseDown, you know capture and bubble; e.OriginalSource is event.target."
---

| C# | In the browser |
| --- | --- |
| `button.Click += handler` | `button.addEventListener("click", handler)` |
| Tunneling `Preview*` events | The capture phase |
| `e.OriginalSource` | `event.target` |
| `e.Handled = true` | `event.stopPropagation()` |
