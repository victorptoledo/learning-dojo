---
from: csharp
status: reviewed
concept: "ASP.NET Core cookie auth and antiforgery for SPAs"
from_way: "JWT bearer authentication for the SPA with the token kept in browser storage."
native_way: "Cookie authentication (or the BFF pattern with OpenID Connect handlers), IAntiforgery with a header for the SPA, and API endpoints answering 401 instead of redirecting."
sensei_tip: "Cookie authentication redirects to the login page by default; override OnRedirectToLogin to return 401 for /api paths."
---

| ASP.NET Core | Across the stack |
| --- | --- |
| `AddCookie(o => o.Cookie.HttpOnly = true)` | session in an HttpOnly cookie |
| `IAntiforgery` + `X-XSRF-TOKEN` | CSRF protection for the SPA |
| `AddOpenIdConnect(...)` | authorization code flow with PKCE, tokens server-side |
| `Events.OnRedirectToLogin` | JSON 401 for API calls |
