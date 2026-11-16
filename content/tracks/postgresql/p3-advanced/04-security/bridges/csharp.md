---
from: csharp
status: reviewed
concept: "Who may touch which rows"
from_way: "ASP.NET Core authorization policies and EF Core global query filters (HasQueryFilter) per tenant, or SQL Server's row-level security."
native_way: "Database roles with least privilege, and row-level security policies with USING and WITH CHECK."
sensei_tip: "EF Core's HasQueryFilter is skipped by IgnoreQueryFilters() and raw SQL. SQL Server RLS uses SESSION_CONTEXT; PostgreSQL uses a custom setting read with current_setting."
---

| C# | PostgreSQL |
| --- | --- |
| [Authorize(Roles = "Admin")] | GRANT on tables to a role |
| modelBuilder.Entity<Invoice>().HasQueryFilter(...) | CREATE POLICY ... USING (...) |
| SESSION_CONTEXT(N'TenantId') (SQL Server) | current_setting('app.tenant_id') |
| a security predicate with a block predicate | USING and WITH CHECK |
| a connection string with db_owner | an API role that owns nothing |
