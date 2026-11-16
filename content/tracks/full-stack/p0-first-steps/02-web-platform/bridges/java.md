---
from: java
status: reviewed
concept: "The browser as a runtime with its own rules"
from_way: "Java web developers know JSP or Thymeleaf forms but leave the DOM and fetch to front-end colleagues."
native_way: "Semantic HTML, real forms, the DOM and fetch are a small API surface, and the browser enforces rules your server cannot."
sensei_tip: "Thymeleaf's th:text escapes and th:utext does not; the same distinction is textContent versus innerHTML in the browser."
---

| Java | Browser |
| --- | --- |
| `th:text="${name}"` | `el.textContent = name` |
| `th:utext` | `el.innerHTML` (dangerous with user text) |
| `HttpClient.send` throws on I/O errors | `fetch` rejects only on network errors, not on 4xx/5xx |
| `@PostMapping` + redirect | a form post answered with 303 |
