---
from: java
status: reviewed
concept: "Spring Security for browsers, not only for APIs"
from_way: "Stateless JWT filters copied from a tutorial, CSRF disabled everywhere, and the SPA keeping the token in localStorage."
native_way: "For a browser client, a session in an HttpOnly cookie with csrf.spa(), or oauth2Login as a backend-for-frontend, and a JSON entry point instead of a login redirect."
sensei_tip: "Disable CSRF only for clients that never use cookies; the moment a browser sends a session cookie, CSRF protection is back on your list."
---

| Spring Security | Across the stack |
| --- | --- |
| `csrf(csrf -> csrf.spa())` | `XSRF-TOKEN` cookie, `X-XSRF-TOKEN` header |
| `oauth2Login()` | a BFF: tokens stay on the server |
| `authenticationEntryPoint(...)` | a JSON 401 the SPA can handle |
| `@PreAuthorize` / ownership checks | the server enforces what the UI shows |
