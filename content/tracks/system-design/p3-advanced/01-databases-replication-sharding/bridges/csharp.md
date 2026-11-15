---
from: csharp
status: draft
concept: "Copies and parts of the data"
from_way: "One DbContext and one SQL Server with identity columns."
native_way: "A primary with readable secondaries, shards chosen by a key, and ids that do not depend on one server."
sensei_tip: "SQL Server Always On readable secondaries are reached with ApplicationIntent=ReadOnly in the connection string; Azure SQL Elastic Database tools provide a shard map (a directory) for tenant routing."
---

| C# / .NET | Replication and sharding concept |
| --- | --- |
| `ApplicationIntent=ReadOnly` | routing to a readable secondary |
| a DbContext per shard from a shard map | directory-based sharding |
| `Guid.CreateVersion7()` | time-ordered ids without identity columns |
| Always On availability groups | leader election and failover |
| Change Data Capture | streaming changes for moves and copies |
