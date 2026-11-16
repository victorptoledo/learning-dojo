---
from: csharp
status: draft
concept: "Testing layers"
from_way: "xUnit or NUnit for units, WebApplicationFactory for integration, Playwright for .NET or Selenium for browsers."
native_way: "JavaScript has the same layers; Playwright's API is nearly identical in both languages."
sensei_tip: "Playwright for .NET and for JavaScript share concepts and names: GetByRole, auto-waiting and tracing transfer one to one."
---

| C# | In the browser |
| --- | --- |
| xUnit | Vitest / node:test |
| `WebApplicationFactory` | A dev server plus Playwright |
| Playwright for .NET `GetByRole` | `getByRole` |
| bUnit for Blazor components | Testing Library component tests |
