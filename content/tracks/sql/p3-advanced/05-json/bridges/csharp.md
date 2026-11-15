---
from: csharp
status: draft
concept: "Documents inside a relational table"
from_way: "System.Text.Json with JsonDocument or POCOs, and EF Core's JSON columns (ToJson)."
native_way: "jsonb with operators and SQL/JSON functions such as JSON_VALUE and JSON_TABLE."
sensei_tip: "EF Core maps owned types to JSON columns with ToJson() and translates queries into them; JSON_VALUE works in both SQL Server and PostgreSQL 17."
---

| C# | SQL |
| --- | --- |
| doc.RootElement.GetProperty("order").GetProperty("id") | payload -> 'order' ->> 'id' |
| JsonSerializer.Serialize(dto) | json_build_object(...) |
| OwnsOne(x => x.Details, b => b.ToJson()) | a JSON column |
| OPENJSON in SQL Server | jsonb_array_elements / JSON_TABLE |
