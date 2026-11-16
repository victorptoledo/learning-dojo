---
from: csharp
status: reviewed
concept: "Plug-ins for the database, and sharing its connections"
from_way: "NuGet packages for features, and Npgsql's built-in pool with Maximum Pool Size=100 by default."
native_way: "CREATE EXTENSION for features inside the database, small client pools, and a pooler in front of the server."
sensei_tip: "Behind PgBouncer in transaction mode, set No Reset On Close=true and avoid Npgsql's automatic preparation on older PgBouncer versions; keep Maximum Pool Size small per instance."
---

| C# | PostgreSQL |
| --- | --- |
| a NuGet package | CREATE EXTENSION |
| Maximum Pool Size=100 | up to 100 processes per instance |
| a shared connection broker | PgBouncer |
| linked servers (SQL Server) | postgres_fdw |
| CLR assemblies in SQL Server | extensions with shared libraries |
