---
from: python
status: reviewed
concept: "Django sessions and CSRF in an SPA world"
from_way: "@csrf_exempt on the API views the SPA calls, or a JWT library with tokens in localStorage."
native_way: "Keep Django's session cookie and CSRF middleware: the SPA reads csrftoken and sends X-CSRFToken; or put Authlib as a BFF in front of the identity provider."
sensei_tip: "CSRF_TRUSTED_ORIGINS must list your SPA's origin when it is on another subdomain, or every POST fails with 403."
---

| Django | Across the stack |
| --- | --- |
| `SESSION_COOKIE_HTTPONLY = True` | the credential is invisible to scripts |
| `csrftoken` cookie + `X-CSRFToken` header | the SPA CSRF pattern |
| `@csrf_exempt` | remove unless no cookie auth is involved |
| object-level permission checks | enforce what the UI hides |
