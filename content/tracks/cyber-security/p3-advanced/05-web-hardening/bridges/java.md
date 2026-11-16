---
from: java
status: draft
concept: "Let the browser enforce security for you"
from_way: "Spring Security defaults left as they are, CORS opened with allowedOrigins(\"*\") and a CSP that nobody set."
native_way: "Spring Security's headers DSL with HSTS, a nonce-based CSP, frame options, and a CorsConfigurationSource with exact origins."
sensei_tip: "Spring Security already sends nosniff, frame options and (over HTTPS) HSTS by default; add `contentSecurityPolicy(...)` yourself, because there is no safe default CSP for every app."
---

| Java | Hardened responses |
| --- | --- |
| allowedOrigins("*") with credentials | allowedOrigins(List.of("https://app.example.com")) |
| no CSP | headers.contentSecurityPolicy(...) |
| frameOptions().disable() | frame-ancestors or DENY |
| server header with version | server.server-header empty |
| JSESSIONID without SameSite | cookie same-site=lax |
