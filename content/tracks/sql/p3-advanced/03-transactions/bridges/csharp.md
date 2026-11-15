---
from: csharp
status: draft
concept: "Concurrent changes that stay correct"
from_way: "lock statements and Interlocked inside one process, TransactionScope and DbContext transactions."
native_way: "Transactions, isolation levels and row locks in the database."
sensei_tip: "EF Core's concurrency tokens ([ConcurrencyCheck], or xmin on PostgreSQL) give you optimistic concurrency; catch DbUpdateConcurrencyException and reload."
---

| C# | SQL |
| --- | --- |
| using var tx = await db.Database.BeginTransactionAsync() | BEGIN |
| lock (sync) { ... } | SELECT ... FOR UPDATE |
| [ConcurrencyCheck] / IsRowVersion() | UPDATE ... WHERE version = :read |
| Interlocked.Add | SET balance = balance - 80 |
| IsolationLevel.Serializable | BEGIN ISOLATION LEVEL SERIALIZABLE |
