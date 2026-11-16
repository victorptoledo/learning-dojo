---
from: python
status: draft
concept: "Let the browser enforce security for you"
from_way: "Flask or Django responses with no security headers, and flask-cors with origins=\"*\" and supports_credentials=True."
native_way: "Django's SecurityMiddleware settings (SECURE_HSTS_SECONDS, SECURE_CONTENT_TYPE_NOSNIFF), a CSP from Django 6's built-in support or a middleware, and exact CORS origins."
sensei_tip: "Django 6.0 added built-in Content Security Policy support (`SECURE_CSP` with nonces in templates); before that, django-csp was the usual choice."
---

| Python | Hardened responses |
| --- | --- |
| no SECURE_* settings | SECURE_HSTS_SECONDS, SECURE_SSL_REDIRECT |
| CORS origins "*" with credentials | an explicit origin list |
| SESSION_COOKIE_SECURE False | True, with SAMESITE Lax |
| X_FRAME_OPTIONS unset | DENY or frame-ancestors |
| Server: gunicorn/23 | hide it at the proxy |
