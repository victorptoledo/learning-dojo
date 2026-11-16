---
from: csharp
status: draft
concept: "Encode for the context the data lands in"
from_way: "@Html.Raw(Model.Bio) and building HTML strings with interpolation."
native_way: "Razor's default encoding, HtmlEncoder/JavaScriptEncoder/UrlEncoder for other contexts, and HtmlSanitizer for user HTML."
sensei_tip: "Razor encodes `@Model.Name` for you; every `@Html.Raw` and `HtmlString` in a code review deserves a question about where the value comes from."
---

| C# | XSS-safe way |
| --- | --- |
| @Html.Raw(Model.Bio) | new HtmlSanitizer().Sanitize(bio) |
| $"<p>{name}</p>" | HtmlEncoder.Default.Encode(name) |
| Regex.IsMatch with ^...$ | anchor with \A and \z |
| href from user input | Uri.TryCreate and a scheme check |
| no CSP | a CSP header in middleware |
