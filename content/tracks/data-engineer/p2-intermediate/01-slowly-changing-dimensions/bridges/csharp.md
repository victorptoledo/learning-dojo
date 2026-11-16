---
from: csharp
status: draft
concept: "Keeping history of changing entities"
from_way: "EF Core entities updated in place, or SQL Server temporal tables for an audit."
native_way: "Type 2 dimension rows with surrogate keys and validity ranges, built by the warehouse load."
sensei_tip: "SQL Server temporal tables (SYSTEM_VERSIONING) keep every version with ValidFrom and ValidTo automatically: a good source for building a type 2 dimension, but they track all columns and use system time."
---

| C# | Slowly changing dimensions |
| --- | --- |
| context.Update(entity) | type 1 |
| temporal table FOR SYSTEM_TIME AS OF | a point-in-time query |
| ValidFrom / ValidTo columns | the validity range |
| SSIS Slowly Changing Dimension transform | type 1 and 2 handling |
| Nullable<T> equality | careful with NULL comparisons |
