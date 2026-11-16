---
from: csharp
status: reviewed
concept: "What sits behind a database connection"
from_way: "NpgsqlConnection with built-in pooling, and EF Core scaffolding the model from the database."
native_way: "Each physical connection is a server process; pg_catalog is the metadata that scaffolding reads."
sensei_tip: "Npgsql pools per connection string: a slightly different string (another Application Name) is another pool. Set Maximum Pool Size deliberately."
---

| C# | PostgreSQL |
| --- | --- |
| new NpgsqlConnection(cs).Open() | a pooled backend process |
| dotnet ef dbcontext scaffold | reads pg_catalog |
| Search Path=billing | SET search_path = billing |
| Maximum Pool Size=100 | up to 100 processes per app instance |
| Application Name=api | application_name in pg_stat_activity |
