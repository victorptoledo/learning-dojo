---
from: java
status: reviewed
concept: "Who may touch which rows"
from_way: "Spring Security roles checked in the service layer, and Hibernate filters (@Filter) or a WHERE tenant_id = ? added to every repository query."
native_way: "Database roles with least privilege, and row-level security policies that filter every query of a role, whatever code sent it."
sensei_tip: "Set the tenant in the same transaction as the queries: a TransactionSynchronization or an aspect that runs SELECT set_config('app.tenant_id', ?, true) right after the transaction begins."
---

| Java | PostgreSQL |
| --- | --- |
| @PreAuthorize("hasRole('ADMIN')") | GRANT on tables to a role |
| a Hibernate @Filter on tenantId | CREATE POLICY ... USING (tenant_id = ...) |
| a ThreadLocal tenant context | SET LOCAL app.tenant_id |
| a DataSource user with full rights | an API role that owns nothing |
| method security tests with @WithMockUser | SET ROLE api; then query |
