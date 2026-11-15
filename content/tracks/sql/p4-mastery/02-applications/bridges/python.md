---
from: python
status: draft
concept: "SQL behind application code"
from_way: "DB-API cursors with %s parameters, SQLAlchemy or Django ORM, and Alembic migrations."
native_way: "Bind parameters, eager loading, keyset pagination and migrations."
sensei_tip: "cursor.execute(sql, params) is safe; f-strings and % formatting of SQL are not. In SQLAlchemy, text() with :name parameters keeps raw SQL safe."
---

| Python | SQL |
| --- | --- |
| cursor.execute('... = %s', (email,)) | a bind parameter |
| f"... = '{email}'" | SQL injection |
| select_related / prefetch_related | a join or a batched = ANY query |
| Paginator(qs, 20).page(n) | OFFSET; prefer a keyset filter |
| alembic revision --autogenerate | a versioned migration |
