---
from: csharp
status: draft
concept: "Persistence model"
from_way: "EF Core change tracking and SaveChanges, with transactions when needed."
native_way: "Isolation, locking and concurrency tokens."
sensei_tip: "EF Core's [ConcurrencyCheck] or a rowversion column is optimistic locking; ExecuteUpdate (EF 7+) issues an atomic UPDATE without loading entities."
---

| C# | In practice |
| --- | --- |
| load, change, SaveChanges | possible lost update |
| rowversion / [Timestamp] | optimistic locking |
| ExecuteUpdateAsync | atomic UPDATE |
| IsolationLevel.Serializable | strongest isolation, more retries |
