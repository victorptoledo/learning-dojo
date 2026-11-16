---
from: csharp
status: draft
concept: "Resolution rules like WPF styles"
from_way: "WPF resolves property values by precedence: local value, triggers, style setters, inherited values, defaults."
native_way: "The CSS cascade is the same idea: a precedence list ending with inheritance and initial values."
sensei_tip: "WPF's dependency property precedence is a cascade; resource dictionaries are like custom properties you can override per scope."
---

| C# | In the browser |
| --- | --- |
| Local value | Inline style |
| Style setters | Rules from stylesheets, ordered by specificity |
| Property value inheritance | Inherited properties |
| DynamicResource | `var(--token)` |
