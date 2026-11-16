---
from: csharp
status: draft
concept: "Components and architecture"
from_way: "Blazor components or WPF user controls with dependency properties and templates."
native_way: "Custom elements are the web's user controls; shadow DOM is the control template; custom properties are theme resources."
sensei_tip: "A WPF ControlTemplate with ContentPresenter is a shadow root with a slot: the visual tree is encapsulated, the content is the user's."
---

| C# | In the browser |
| --- | --- |
| UserControl | A custom element |
| ControlTemplate | The shadow root's template |
| ContentPresenter | `<slot>` |
| Theme ResourceDictionary | Custom properties (design tokens) |
