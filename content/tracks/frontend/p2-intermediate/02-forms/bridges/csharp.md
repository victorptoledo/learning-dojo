---
from: csharp
status: draft
concept: "Validation at the edge"
from_way: "Data annotations ([Required], [EmailAddress], [StringLength]) and ModelState in ASP.NET Core."
native_way: "The browser enforces the matching HTML constraints and exposes them through ValidityState."
sensei_tip: "ASP.NET tag helpers already emit data-val attributes for jQuery validation; the native constraint attributes do the same job without a library."
---

| C# | In the browser |
| --- | --- |
| `[Required]` | `required` |
| `[EmailAddress]` | `type="email"` |
| `[StringLength(500, MinimumLength = 10)]` | `minlength="10" maxlength="500"` |
| `ModelState.IsValid` | `form.checkValidity()` (and the server's own check) |
