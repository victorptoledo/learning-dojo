---
from: csharp
status: draft
concept: "Security on the client side"
from_way: "Razor's automatic HTML encoding, antiforgery tokens and CORS policies in ASP.NET Core."
native_way: "The browser enforces CSP, CORS and cookie rules that ASP.NET Core configures; client code must still avoid HTML sinks."
sensei_tip: "@Html.Raw is innerHTML: if you would not use it in Razor, do not use innerHTML in JavaScript."
---

| C# | In the browser |
| --- | --- |
| Razor `@value` encoding | `textContent` |
| `@Html.Raw(value)` | `innerHTML` |
| `[ValidateAntiForgeryToken]` | CSRF tokens plus SameSite cookies |
| `builder.Services.AddCors(...)` | The `Access-Control-Allow-*` headers |
