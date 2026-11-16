---
from: java
status: draft
concept: "HTTP caching and conditional requests"
from_way: "`@Cacheable` in the service layer and no HTTP caching headers at all."
native_way: "`ResponseEntity.ok().eTag(...).lastModified(...).cacheControl(CacheControl.noCache().cachePrivate())`; `If-Match` checked against a `@Version` column."
sensei_tip: "`ShallowEtagHeaderFilter` hashes every response body: it saves bandwidth, not work. A version-based ETag lets you answer 304 before loading anything heavy."
---

| Java / Spring | HTTP caching |
| --- | --- |
| `ResponseEntity...eTag("\"7\"")` | ETag |
| `CacheControl.maxAge(Duration.ofMinutes(5))` | Cache-Control |
| `WebRequest.checkNotModified(etag)` | 304 handling |
| `@Version long version` | source of the ETag and optimistic locking |
| `headers.getIfMatch()` | conditional write |
