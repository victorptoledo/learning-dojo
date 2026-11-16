---
from: javascript
status: reviewed
concept: "Who may touch which rows"
from_way: "Middleware checking roles, and a where: { tenantId } added to every Prisma or Knex query."
native_way: "Database roles with least privilege, and row-level security policies that filter every query."
sensei_tip: "Supabase is built on exactly this: RLS policies using auth.uid(). In your own app, run set_config('app.tenant_id', id, true) as the first statement of every transaction."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| a role check in Express middleware | GRANT on tables to a role |
| where: { tenantId } on every query | CREATE POLICY ... USING (tenant_id = ...) |
| AsyncLocalStorage with the tenant | SET LOCAL app.tenant_id inside the transaction |
| one DATABASE_URL with the owner | an API role that owns nothing |
| Supabase auth.uid() in policies | current_setting('app.user_id') in policies |
