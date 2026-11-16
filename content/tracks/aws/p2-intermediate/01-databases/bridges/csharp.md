---
from: csharp
status: draft
concept: "Pick the store from the access patterns"
from_way: "Entity Framework Core on SQL Server, with LINQ queries written freely."
native_way: "RDS (SQL Server or PostgreSQL) or Aurora for relational data, DynamoDB for known access patterns at any scale."
sensei_tip: "The `DynamoDBContext` object persistence model maps classes with `[DynamoDBHashKey]` and `[DynamoDBRangeKey]`; keep the key design in one place and test it against DynamoDB Local."
---

| .NET world | AWS |
| --- | --- |
| `DbContext` and LINQ | Query and GetItem by key |
| `[ConcurrencyCheck]` | a condition expression |
| SQL Server Always On | RDS Multi-AZ or Aurora replicas |
| `TransactionScope` | `TransactWriteItems` |
