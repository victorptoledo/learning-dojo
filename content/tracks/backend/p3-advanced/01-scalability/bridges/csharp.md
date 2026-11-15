---
from: csharp
status: draft
concept: "From one server to many"
from_way: "Scaling up a server or a web farm with shared session state."
native_way: "Stateless instances, caching and data partitioning."
sensei_tip: "ASP.NET Core's data protection keys must be shared across instances, or cookies created by one instance fail on another."
---

| C# | In practice |
| --- | --- |
| in-proc session | distributed cache session |
| data protection keys on disk | shared key ring |
| a bigger SQL Server | read replicas, then sharding |
| IMemoryCache per instance | a shared cache when consistency matters |
