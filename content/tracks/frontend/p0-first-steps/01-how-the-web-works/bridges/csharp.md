---
from: csharp
status: draft
concept: "The browser as a client runtime"
from_way: "ASP.NET MVC or Razor Pages render HTML on the server; Blazor Server keeps the UI state on the server."
native_way: "Plain web pages render in the browser from HTML, CSS and JavaScript, and the browser's DevTools are your debugger."
sensei_tip: "Razor views produce exactly the HTML this track teaches; whatever generates it, the browser only sees elements, styles and scripts."
---

| C# | In the browser |
| --- | --- |
| Razor `.cshtml` | The HTML document |
| `HttpClient` | `fetch` |
| Visual Studio debugger | DevTools' Sources panel and breakpoints |
| `Console.WriteLine` | `console.log` |
