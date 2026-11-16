---
from: python
status: draft
concept: "Security on the client side"
from_way: "Django's auto-escaping templates, CsrfViewMiddleware, SecurityMiddleware and django-cors-headers."
native_way: "In client-rendered code there is no template auto-escaping unless you use textContent or a framework that escapes."
sensei_tip: "Django escapes {{ value }} for you; innerHTML is |safe on everything. Keep the same instinct in JavaScript."
---

| Python | In the browser |
| --- | --- |
| Template auto-escaping | `textContent` |
| `mark_safe` / `|safe` | `innerHTML` (dangerous) |
| `CSRF_COOKIE_SAMESITE`, `SESSION_COOKIE_HTTPONLY` | Cookie attributes |
| django-csp | The `Content-Security-Policy` header |
