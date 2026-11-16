---
from: csharp
status: reviewed
concept: "Types that carry rules"
from_way: "C# enums, value objects with validation, List<string> properties and DateTimeOffset."
native_way: "Enums, domains, arrays and ranges in the database, with exclusion constraints that hold for every client."
sensei_tip: "Npgsql maps PostgreSQL enums (MapEnum), arrays (string[]) and ranges (NpgsqlRange<DateTime>) natively, and EF Core can create exclusion constraints through raw migrations."
---

| C# | PostgreSQL |
| --- | --- |
| enum OrderStatus | ENUM, or text with CHECK |
| record Email with a validating constructor | CREATE DOMAIN email ... |
| string[] / List<string> | text[] |
| DateTimeOffset / DateTime (Kind Utc) | timestamptz |
| NpgsqlRange<DateTime> | tstzrange |
| a lock around "is it free?" | EXCLUDE USING gist |
