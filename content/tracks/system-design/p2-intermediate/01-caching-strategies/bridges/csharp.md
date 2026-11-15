---
from: csharp
status: draft
concept: "Keeping a second copy close"
from_way: "IMemoryCache in one instance and IDistributedCache backed by Redis."
native_way: "Two tiers with different staleness, a delete-on-write rule, TTLs with jitter and protection against stampedes."
sensei_tip: ".NET 9's HybridCache combines an in-memory and a distributed tier and coalesces concurrent loads of the same key; it is the stampede protection you would otherwise write yourself."
---

| C# / .NET | Caching concept |
| --- | --- |
| `IMemoryCache.GetOrCreateAsync` | in-process cache-aside |
| `MemoryCacheEntryOptions.SlidingExpiration` | sliding TTL |
| `IDistributedCache` + StackExchange.Redis | the shared tier |
| `HybridCache` | two tiers with stampede protection |
| `cache.Remove(key)` after `SaveChangesAsync` | delete on write |
