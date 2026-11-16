---
from: python
status: reviewed
concept: "Conditional views and cache headers in Django"
from_way: "No cache headers at all, or @cache_page on a view that shows per-user data."
native_way: "@condition(etag_func=...) for 304s and 412s, cache_control(private=True) for user data, and a client cache in the SPA that revalidates."
sensei_tip: "@cache_page on a view that reads request.user caches one user's page for everyone unless you vary on the cookie: prefer private headers."
---

| Django | Data fetching |
| --- | --- |
| `@condition(etag_func=product_etag)` | 304 on GET, 412 on stale writes |
| `@cache_control(private=True, no_cache=True)` | per-user, revalidated |
| `@cache_page(60)` | shared caching: public data only |
| `patch_vary_headers(response, ["Origin"])` | correct CORS caching |
