---
from: go
status: draft
concept: "Creating, changing and removing stored records"
from_way: "db.ExecContext returning sql.Result with RowsAffected, and QueryRow(...).Scan for RETURNING."
native_way: "INSERT, UPDATE and DELETE statements, RETURNING generated keys, ON CONFLICT for upserts."
sensei_tip: "PostgreSQL drivers do not support LastInsertId; use INSERT ... RETURNING id with QueryRow and Scan. Check RowsAffected after updates."
---

| Go | SQL |
| --- | --- |
| db.ExecContext(ctx, "UPDATE ...") | UPDATE ... WHERE ... |
| res.RowsAffected() | the row count |
| db.QueryRowContext(ctx, "INSERT ... RETURNING id").Scan(&id) | INSERT ... RETURNING id |
| pgx CopyFrom | COPY for bulk inserts |
