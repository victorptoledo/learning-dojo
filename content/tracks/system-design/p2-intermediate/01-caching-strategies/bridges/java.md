---
from: java
status: draft
concept: "Keeping a second copy close"
from_way: "Spring's @Cacheable on a service method, with a ConcurrentMapCache or Caffeine behind it."
native_way: "A cache is a design decision with a staleness budget, a fill pattern, an eviction policy and a failure mode, often in two tiers: in-process and distributed."
sensei_tip: "Caffeine's refreshAfterWrite gives you stale-while-revalidate, and its LoadingCache coalesces concurrent loads of the same key, which is single flight for free. You build a simpler version in the next kata."
---

| Java / Spring | Caching concept |
| --- | --- |
| `@Cacheable("products")` | cache-aside / read-through |
| `@CacheEvict(key = "#id")` on update | delete on write |
| Caffeine `expireAfterWrite(5, MINUTES)` | TTL expiration |
| Caffeine `maximumSize(10_000)` | W-TinyLFU eviction |
| `LinkedHashMap(cap, 0.75f, true)` + `removeEldestEntry` | an LRU cache in a dozen lines |
| Spring Data Redis `RedisCacheManager` | the distributed tier |
