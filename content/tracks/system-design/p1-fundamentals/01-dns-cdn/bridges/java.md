---
from: java
status: draft
concept: "Routing and caching at the edge"
from_way: "Picturing every request hitting the Spring Boot application directly, with caching done in the service layer."
native_way: "Most requests never reach the JVM: DNS picks a region, anycast picks a nearby PoP, and the CDN answers from its cache using the HTTP headers your controllers send."
sensei_tip: "The JVM caches DNS answers itself (networkaddress.cache.ttl in the java.security file, 30 s by default). Check it before a DNS failover, or your clients keep the old address."
---

| Java / Spring | Edge concept |
| --- | --- |
| `ResponseEntity.ok().cacheControl(CacheControl.maxAge(...))` | the `Cache-Control` the CDN obeys |
| `ShallowEtagHeaderFilter` | `ETag` and `304 Not Modified` revalidation |
| `InetAddress.getByName(host)` | a DNS lookup, cached by the JVM |
| `spring.web.resources.chain.strategy.content` | fingerprinted static assets |
| `@Cacheable` | a cache *behind* the edge, not in front of it |
