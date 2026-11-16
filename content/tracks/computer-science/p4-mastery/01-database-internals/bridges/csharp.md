---
from: csharp
status: draft
concept: "Database internals seen from C#"
from_way: "Leaving TransactionScope defaults unexamined."
native_way: "TransactionScope defaults to Serializable on SQL Server; EF Core tracks changes and writes them in one transaction on SaveChanges; SQL Server uses locking by default and row versioning with READ_COMMITTED_SNAPSHOT."
sensei_tip: "Pick the isolation level deliberately (TransactionOptions), and use EF Core's AsNoTracking for read-only queries."
---

| In C# | Underneath |
| --- | --- |
| `TransactionScope` | Serializable by default |
| `SaveChanges()` | one transaction, many statements |
| `READ_COMMITTED_SNAPSHOT` | MVCC-style reads on SQL Server |
| `AsNoTracking()` | no change tracking for reads |
| `DbUpdateConcurrencyException` | optimistic concurrency failed |
