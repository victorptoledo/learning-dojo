---
from: csharp
status: draft
concept: "Accessibility as an interface contract"
from_way: "WPF's AutomationProperties and UI Automation expose names and roles to Narrator."
native_way: "The web's equivalent is the accessibility tree, built from HTML and ARIA and read by NVDA, JAWS, Narrator and VoiceOver."
sensei_tip: "AutomationProperties.Name is aria-label; LabeledBy is aria-labelledby; and Narrator works with web pages too, so test with it."
---

| C# | In the browser |
| --- | --- |
| `AutomationProperties.Name` | `aria-label` |
| `AutomationProperties.LabeledBy` | `aria-labelledby` |
| `IsTabStop`, `TabIndex` | Native focusability, `tabindex="0"` or `-1` |
| UI Automation tree | The browser's accessibility tree |
