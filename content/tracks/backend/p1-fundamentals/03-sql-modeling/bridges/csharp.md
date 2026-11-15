---
from: csharp
status: draft
concept: "Thinking in tables"
from_way: "EF Core entities with conventions and Fluent API configuration."
native_way: "Tables, constraints and indexes designed deliberately."
sensei_tip: "HasIndex(...).IsUnique() and HasCheckConstraint are where EF Core meets this lesson. Read the migration's SQL before you apply it."
---

| C# | In practice |
| --- | --- |
| navigation properties | foreign keys |
| HasIndex(...).IsUnique() | UNIQUE index |
| HasCheckConstraint | CHECK |
| owned types | columns or a separate table |
