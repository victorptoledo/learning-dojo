---
from: csharp
status: reviewed
concept: "Code that runs next to the data"
from_way: "T-SQL stored procedures and triggers on SQL Server, or EF Core SaveChanges interceptors."
native_way: "PL/pgSQL functions, procedures and triggers; triggers call a separate trigger function."
sensei_tip: "Coming from T-SQL: PostgreSQL triggers can be FOR EACH ROW (T-SQL triggers are per statement with inserted and deleted tables, which match PostgreSQL's transition tables)."
---

| C# / T-SQL | PostgreSQL |
| --- | --- |
| SaveChanges interceptor setting UpdatedAt | BEFORE UPDATE row trigger |
| the inserted and deleted pseudo-tables | REFERENCING NEW TABLE / OLD TABLE |
| THROW 50001, 'msg', 1 | RAISE EXCEPTION 'msg' USING ERRCODE = ... |
| BEGIN TRY / BEGIN CATCH | BEGIN ... EXCEPTION WHEN ... THEN |
| EXECUTE AS OWNER | SECURITY DEFINER with SET search_path |
