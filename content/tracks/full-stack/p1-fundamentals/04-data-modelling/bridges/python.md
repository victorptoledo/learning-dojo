---
from: python
status: reviewed
concept: "From Django models to the queries a page runs"
from_way: "Model fields generate the schema, and templates that touch order.items.all() inside a loop cause N+1."
native_way: "Migrations are reviewed like code, constraints are declared in Meta.constraints, and list views use select_related, prefetch_related or one SQL query."
sensei_tip: "django-debug-toolbar or nplusone in tests make N+1 visible before production does."
---

| Python habit | Full stack practice |
| --- | --- |
| `for o in orders: o.items.all()` | `prefetch_related("items")` |
| `Paginator` (offset) | keyset pagination with a cursor |
| `makemigrations` unseen | reviewed, forward-only migrations |
| `clean()` only | `CheckConstraint` in the database too |
