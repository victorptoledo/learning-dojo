---
from: java
status: reviewed
concept: "HTTP caching from Spring MVC"
from_way: "Every response sent with no caching headers, and @Version used only to fail with an exception page."
native_way: "CacheControl on ResponseEntity, ETags from the entity version with checkNotModified, and If-Match mapped to 412 so the UI can show a conflict."
sensei_tip: "WebRequest.checkNotModified(etag) answers 304 before you serialise anything; ShallowEtagHeaderFilter only saves bandwidth."
---

| Spring | Data fetching |
| --- | --- |
| `ResponseEntity.ok().cacheControl(CacheControl.noCache()).eTag(v)` | revalidated answers |
| `request.checkNotModified(etag)` | 304 Not Modified |
| `@Version` + `If-Match` | 412 instead of a lost update |
| `@Cacheable` | a server cache: plan its eviction |
