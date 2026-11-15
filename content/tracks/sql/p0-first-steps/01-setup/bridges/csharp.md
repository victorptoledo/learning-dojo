---
from: csharp
status: draft
concept: "Asking a database for data instead of looping over it"
from_way: "LINQ over a List<T>, or LINQ to Entities that Entity Framework translates to SQL."
native_way: "SQL itself: LINQ's query syntax was modelled on it, so most ideas map one to one."
sensei_tip: "Use ToQueryString() or EF logging to see the SQL your LINQ becomes; it is the fastest way to learn both."
---

| C# | SQL |
| --- | --- |
| customers.Where(c => c.Country == "PT") | WHERE country = 'PT' |
| from c in customers select c.Name | SELECT name FROM customers |
| .OrderBy(c => c.Name) | ORDER BY name |
| new NpgsqlConnection(cs) | psql -d dojo |
