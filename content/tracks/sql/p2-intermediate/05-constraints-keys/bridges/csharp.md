---
from: csharp
status: draft
concept: "Rules the data itself enforces"
from_way: "Data annotations ([Required], [Range]), FluentValidation, and HasIndex(...).IsUnique() in EF Core."
native_way: "Database constraints, which EF Core migrations can create from the model."
sensei_tip: "EF Core creates foreign keys and unique indexes from the model, but CHECK constraints need HasCheckConstraint. Catch DbUpdateException and look at the PostgresException SqlState."
---

| C# | SQL |
| --- | --- |
| [Required] | NOT NULL |
| [Range(1, int.MaxValue)] | CHECK (seats > 0) |
| HasIndex(c => c.Email).IsUnique() | UNIQUE (email) |
| OnDelete(DeleteBehavior.Cascade) | ON DELETE CASCADE |
| PostgresException.SqlState == "23505" | unique_violation |
