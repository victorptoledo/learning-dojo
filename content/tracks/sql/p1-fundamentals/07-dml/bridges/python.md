---
from: python
status: draft
concept: "Creating, changing and removing stored records"
from_way: "ORM sessions (session.add, session.commit), or cursor.execute with executemany for many rows."
native_way: "Set-based INSERT, UPDATE and DELETE statements with RETURNING and ON CONFLICT."
sensei_tip: "cursor.rowcount tells you how many rows a statement touched; check it. For many rows, executemany or COPY is far faster than a loop of execute."
---

| Python | SQL |
| --- | --- |
| session.add(obj); session.commit() | INSERT ...; COMMIT |
| for p in ps: p.price *= 1.1 | UPDATE products SET price = price * 1.1 WHERE ... |
| cursor.rowcount | UPDATE 37 |
| dict.setdefault / get-or-create | INSERT ... ON CONFLICT DO NOTHING |
| cursor.executemany(sql, rows) | a multi-row INSERT or COPY |
