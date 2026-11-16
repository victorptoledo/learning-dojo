---
from: python
status: reviewed
concept: "Who may touch which rows"
from_way: "Django permissions and a tenant filter on every queryset, often in a custom manager."
native_way: "Database roles with least privilege, and row-level security policies that apply to every query of a role."
sensei_tip: "A custom manager that filters by tenant is skipped by raw SQL, admin actions and forgotten querysets; RLS is not. Run SET LOCAL in a middleware inside transaction.atomic()."
---

| Python | PostgreSQL |
| --- | --- |
| user.has_perm('billing.view_invoice') | GRANT SELECT ON invoices TO a role |
| Invoice.objects.filter(tenant=request.tenant) | a policy USING (tenant_id = ...) |
| a contextvar holding the tenant | SET LOCAL app.tenant_id |
| DATABASES user = the table owner | a separate role that owns nothing |
| tests that log in as a normal user | SET ROLE api before the queries |
