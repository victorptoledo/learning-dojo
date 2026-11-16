---
from: csharp
status: draft
concept: "bUnit with semantics"
from_way: "bUnit renders Blazor components and finds elements with CSS selectors (cut.Find(\"button\")); Playwright for .NET drives browsers with GetByRole."
native_way: "Testing Library is React's bUnit, but it steers you to role and label queries, like Playwright's GetByRole; user-event replaces Click() helpers."
sensei_tip: "You may already use Playwright's GetByRole in .NET: Testing Library uses the same idea at the component level, so the same accessible markup serves both."
---

| .NET | React |
| --- | --- |
| bUnit `RenderComponent<T>()` | `render(<T />)` |
| `cut.Find("button").Click()` | `await user.click(screen.getByRole("button", { name }))` |
| Playwright `GetByRole` | `getByRole` |
| `WaitForAssertion` | `waitFor` |
| Moq at the boundary | Fake fetch or loader props |
| Accessibility Insights | axe, NVDA, browser accessibility tree |
