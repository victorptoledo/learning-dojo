---
from: csharp
status: reviewed
concept: "Documents and search inside the database"
from_way: "System.Text.Json documents, SQL Server's JSON_VALUE and full-text catalogs, or a separate search service."
native_way: "jsonb with containment and jsonpath operators and GIN indexes, and built-in full-text search with tsvector and tsquery."
sensei_tip: "Npgsql's EF Core provider maps owned entities to jsonb (ToJson()) and translates EF.Functions.JsonContains to @>. Its full-text functions (EF.Functions.ToTsVector, Matches) translate to to_tsvector and @@."
---

| C# | PostgreSQL |
| --- | --- |
| JsonDocument.RootElement.GetProperty("brand") | attrs ->> 'brand' |
| JSON_VALUE(attrs, '$.brand') (SQL Server) | attrs ->> 'brand' or jsonb_path_query |
| EF.Functions.JsonContains(p.Attrs, json) | attrs @> json |
| CONTAINS(body, 'login') (SQL Server) | doc @@ to_tsquery('english', 'login') |
| a full-text catalog | a GIN index on a tsvector |
