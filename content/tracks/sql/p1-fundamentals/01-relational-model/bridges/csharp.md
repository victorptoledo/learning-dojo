---
from: csharp
status: draft
concept: "Tables, types and keys versus classes, fields and references"
from_way: "A class with properties, navigation properties for relations, and decimal for money."
native_way: "A table with columns, foreign keys and numeric(p, s) for money."
sensei_tip: "EF Core migrations generate DDL from your classes: read the generated SQL and fix types (precision of decimals, max lengths) before you ship it."
---

| C# | SQL |
| --- | --- |
| public Customer Customer { get; set; } | customer_id REFERENCES customers (id) |
| decimal Price | price numeric(10, 2) |
| DateTimeOffset CreatedAt | created_at timestamptz |
| string? Nickname | nickname text (nullable) |
| [Key] public long Id | id bigint PRIMARY KEY |
