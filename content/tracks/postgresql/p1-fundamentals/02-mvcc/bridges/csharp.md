---
from: csharp
status: reviewed
concept: "Readers that never block writers"
from_way: "Immutable records with with-expressions (row with { Balance = 80 }) and the .NET garbage collector."
native_way: "MVCC: every update writes a new row version, each transaction reads the versions its snapshot allows, and VACUUM collects the garbage."
sensei_tip: "EF Core with Npgsql can use xmin as a concurrency token (UseXminAsConcurrencyToken or [Timestamp] on a uint xmin property): it changes on every update, for free."
---

| C# | PostgreSQL |
| --- | --- |
| row with { Balance = 80 } | UPDATE writes a new tuple |
| an old reference still in use | a snapshot seeing old versions |
| the GC | VACUUM |
| a TransactionScope around a slow HTTP call | idle in transaction |
| [Timestamp] uint xmin | optimistic concurrency on the row version |
