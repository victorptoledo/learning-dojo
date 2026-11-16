---
from: java
status: draft
concept: "Security on the client side"
from_way: "Spring Security: CSRF tokens, CORS configuration, security headers and session cookies set by the server."
native_way: "The browser enforces those policies; the frontend must not open XSS holes that bypass them all from inside the origin."
sensei_tip: "Spring Security's headers() and cors() are where the server side of this lesson lives; the frontend's job is textContent, URL checks and no inline scripts."
---

| Java | In the browser |
| --- | --- |
| `http.csrf()` | SameSite cookies plus tokens or Origin checks |
| `CorsConfiguration.setAllowedOrigins` | `Access-Control-Allow-Origin` |
| `headers().contentSecurityPolicy(...)` | The `Content-Security-Policy` header |
| OWASP Java Encoder | `textContent` / a sanitizer for HTML |
