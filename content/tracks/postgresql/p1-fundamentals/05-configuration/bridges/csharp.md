---
from: csharp
status: reviewed
concept: "Layered configuration"
from_way: "IConfiguration with appsettings.json, appsettings.Production.json, environment variables and command-line providers."
native_way: "Defaults, postgresql.conf, ALTER SYSTEM, database, role, session and transaction settings, later ones winning."
sensei_tip: "Npgsql's Command Timeout is client-side; statement_timeout is enforced by the server for every client. Set both."
---

| C# | PostgreSQL |
| --- | --- |
| appsettings.json | postgresql.conf |
| appsettings.Production.json | ALTER DATABASE ... SET |
| environment variables | ALTER SYSTEM |
| Options=-c work_mem=64MB in the connection string | a session setting |
| CommandTimeout | client-side only; statement_timeout is server-side |
