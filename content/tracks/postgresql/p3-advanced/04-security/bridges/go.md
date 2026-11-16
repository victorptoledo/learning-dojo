---
from: go
status: reviewed
concept: "Who may touch which rows"
from_way: "Middleware putting the tenant in context.Context and every repository function adding WHERE tenant_id = $1."
native_way: "Database roles with least privilege, and row-level security policies that filter every query."
sensei_tip: "Start every transaction with tx.Exec(ctx, \"SELECT set_config('app.tenant_id', $1, true)\", tenantID) in one helper, so no handler can forget it."
---

| Go | PostgreSQL |
| --- | --- |
| ctx value with the tenant | SET LOCAL app.tenant_id |
| WHERE tenant_id = $1 in every query | CREATE POLICY ... USING (...) |
| a role check in middleware | GRANT on tables to a role |
| one DSN with the owner | an API role that owns nothing |
| tests with a non-admin user | SET ROLE api before the queries |
