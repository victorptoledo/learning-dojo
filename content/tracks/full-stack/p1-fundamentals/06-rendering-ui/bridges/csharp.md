---
from: csharp
status: reviewed
concept: "Razor Pages, Blazor and plain SPAs"
from_way: "Razor Pages or MVC for everything, or Blazor WebAssembly even for public content pages."
native_way: "Razor Pages for content and forms (with tag helpers for errors), interactive islands with Blazor or JavaScript where needed, and PRG after posts."
sensei_tip: "Razor Pages' OnPost returning Page() with ModelState errors, and RedirectToPage on success, is PRG with field errors out of the box."
---

| ASP.NET Core | Rendering practice |
| --- | --- |
| `return Page()` with `ModelState` errors | the form again with errors and values |
| `RedirectToPage("Index")` | Post/Redirect/Get |
| `<span asp-validation-for="Title">` | error under the field |
| `@Html.Raw` | raw HTML: review it |
