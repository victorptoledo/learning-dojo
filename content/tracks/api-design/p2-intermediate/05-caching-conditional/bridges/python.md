---
from: python
status: draft
concept: "HTTP caching and conditional requests"
from_way: "No caching headers; `functools.lru_cache` on the view."
native_way: "Set `ETag`, `Last-Modified` and `Cache-Control` on the `Response`; compare `If-None-Match` and `If-Match` in the handler or a dependency."
sensei_tip: "Django has `@condition(etag_func=..., last_modified_func=...)`, which implements 304 and 412 for you."
---

| Python | HTTP caching |
| --- | --- |
| `response.headers["ETag"] = '"7"'` | ETag |
| `Cache-Control: private, no-cache` | revalidate every time |
| `request.headers.get("if-none-match")` | conditional read |
| Django `@condition` | 304 and 412 built in |
| `version` column | optimistic locking |
