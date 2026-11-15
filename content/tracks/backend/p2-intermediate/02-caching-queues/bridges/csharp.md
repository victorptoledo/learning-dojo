---
from: csharp
status: draft
concept: "Non-blocking operations"
from_way: "IMemoryCache, IDistributedCache with Redis and HybridCache, plus background services."
native_way: "Cache-aside, TTLs and stampede protection, and queues."
sensei_tip: "HybridCache (.NET 9) already provides stampede protection for a key; with IDistributedCache you need your own lock or single flight."
---

| C# | In practice |
| --- | --- |
| IMemoryCache.GetOrCreate | cache-aside |
| HybridCache | cache-aside with stampede protection |
| IDistributedCache | a shared cache |
| BackgroundService + Channel | a work queue |
