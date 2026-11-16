---
from: python
status: reviewed
concept: "django-cors-headers and cookie settings"
from_way: "CORS_ALLOW_ALL_ORIGINS = True in settings and SESSION_COOKIE_SECURE left False."
native_way: "List the allowed origins, enable credentials only with them, and set the cookie flags for every environment except local development."
sensei_tip: "With FastAPI's CORSMiddleware, allow_credentials=True and allow_origins=['*'] do not work together in browsers: list the origins."
---

| Python | Browser effect |
| --- | --- |
| `CORS_ALLOWED_ORIGINS = ["https://app.example.com"]` | only that origin may read answers |
| `SESSION_COOKIE_HTTPONLY = True` | scripts cannot read the session |
| `SESSION_COOKIE_SAMESITE = "Lax"` | no cross-site POSTs with the cookie |
| `redirect(..., status=303)` | the browser follows with a GET |
