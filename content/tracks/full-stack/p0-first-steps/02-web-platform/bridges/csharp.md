---
from: csharp
status: reviewed
concept: "From Razor and tag helpers to the raw platform"
from_way: "Razor tag helpers generate forms and validation attributes, so the HTML itself stays hidden."
native_way: "Read the HTML they generate: method, action, names and encoding are what the browser sends, and the DOM is what scripts change."
sensei_tip: "@Html.Raw is the C# twin of innerHTML; search for it in code review like you search for string-built SQL."
---

| .NET | Browser |
| --- | --- |
| `@Model.Name` (encoded) | `el.textContent = name` |
| `@Html.Raw(x)` | `el.innerHTML = x` |
| `response.EnsureSuccessStatusCode()` | `if (!response.ok) throw ...` |
| `<form asp-action="Create">` | `<form method="post" action="/orders">` |
