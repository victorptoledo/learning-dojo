---
from: csharp
status: draft
concept: "Layout panels"
from_way: "WPF's StackPanel, WrapPanel, Grid with star sizing, and adaptive triggers in WinUI."
native_way: "Flexbox is StackPanel plus WrapPanel; CSS grid is WPF's Grid where `*` sizes become `fr`; media queries are adaptive triggers."
sensei_tip: "Star sizing (`2*`) is exactly `2fr`, and `Auto` is `auto`: your WPF Grid intuition transfers directly."
---

| C# | In the browser |
| --- | --- |
| `StackPanel` | `display: flex` |
| `WrapPanel` | `flex-wrap: wrap` |
| `<ColumnDefinition Width="2*"/>` | `2fr` |
| `AdaptiveTrigger MinWindowWidth` | `@media (min-width: ...)` |
