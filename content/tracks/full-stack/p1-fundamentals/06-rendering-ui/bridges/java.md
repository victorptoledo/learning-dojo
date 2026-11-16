---
from: java
status: reviewed
concept: "Thymeleaf pages next to an SPA"
from_way: "Either everything in Thymeleaf with full reloads, or a React SPA for everything including static pages."
native_way: "Choose per screen: Thymeleaf (or htmx on top) for content pages and simple forms, an SPA for tool-like screens, with PRG and errors under fields in both."
sensei_tip: "BindingResult plus th:errors gives you errors under fields; on success return \"redirect:/orders\" (Spring sends 302; use 303 with a RedirectView if you want to be strict)."
---

| Spring MVC | Rendering practice |
| --- | --- |
| `return "orders"` with a `Model` | server-rendered page |
| `@Valid` + `BindingResult` + `th:errors` | 422-style page with field errors and typed values |
| `"redirect:/orders"` | Post/Redirect/Get |
| `th:utext` | raw HTML: review it |
