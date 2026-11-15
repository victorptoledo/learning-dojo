---
from: csharp
status: draft
concept: "Agreeing on one owner"
from_way: "lock and SemaphoreSlim in one process, Hangfire or Quartz on one server."
native_way: "Leases in a consensus-backed store or blob leases, renewed in the background, with fencing tokens on writes."
sensei_tip: "Azure Blob Storage leases are a ready-made lease service (15–60 s or infinite); combine one with a monotonically increasing token stored in your database, since a blob lease alone does not fence."
---

| C# / .NET | Consensus concept |
| --- | --- |
| `lock` / `SemaphoreSlim` | mutual exclusion in one process |
| `BlobLeaseClient.AcquireAsync` | a lease from blob storage |
| dotnet-etcd lease APIs | etcd leases |
| EF Core concurrency tokens (`[ConcurrencyCheck]`) | conditional, fencing-like writes |
| `Stopwatch.GetTimestamp()` | the monotonic clock |
