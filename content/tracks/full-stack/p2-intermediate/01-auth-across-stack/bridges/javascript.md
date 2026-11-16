---
from: javascript
status: reviewed
concept: "Where the SPA keeps its login"
from_way: "localStorage.setItem(\"token\", jwt) and an axios interceptor that adds the Bearer header."
native_way: "No token in JavaScript: the server sets an HttpOnly session cookie (or a BFF holds the tokens), fetch uses credentials: \"include\" and sends the CSRF header on writes."
sensei_tip: "A 401 interceptor that redirects to /login with a returnTo parameter is the SPA side of a JSON 401 entry point."
---

| SPA habit | Safer pattern |
| --- | --- |
| token in `localStorage` | `HttpOnly` cookie or a BFF |
| `Authorization: Bearer` from JS | cookies sent by the browser |
| no CSRF on writes | `X-XSRF-TOKEN` read from a cookie |
| hiding buttons as security | server-side checks, `canDelete` flags from the API |
