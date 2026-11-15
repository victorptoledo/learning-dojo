---
from: csharp
status: draft
concept: "Spreading load and owning keys"
from_way: "Dictionary<TKey,TValue> buckets by hash code; IIS or Kestrel behind Azure App Service with ARR affinity."
native_way: "Balancers spread stateless requests by health and load; keyed tiers place data on a consistent hash ring."
sensei_tip: "string.GetHashCode() is randomized per process in .NET Core, so it cannot place keys across servers. Use a stable hash (XxHash64 from System.IO.Hashing, or MD5) for rings."
---

| C# / .NET | Load balancing concept |
| --- | --- |
| `string.GetHashCode()` | per-process hash: unusable for placement |
| `SortedList` + binary search | a consistent hash ring |
| ARR affinity cookie | sticky sessions, best turned off |
| `MapHealthChecks("/health")` | instance health endpoint |
| YARP reverse proxy | an L7 balancer written in .NET |
