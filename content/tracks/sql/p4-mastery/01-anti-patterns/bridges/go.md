---
from: go
status: draft
concept: "Queries that look right and are not"
from_way: "Pitfalls like ignoring errors, loop variable capture and float money."
native_way: "SQL pitfalls: NULL logic, fan-out, row-by-row work, OFFSET, BETWEEN on timestamps."
sensei_tip: "A query per item inside a range loop is the most common Go database anti-pattern; collect the ids and send them as one array parameter."
---

| Go | SQL |
| --- | --- |
| for _, id := range ids { db.QueryRow(...) } | WHERE id = ANY($1) |
| float64 for prices | numeric with a decimal type |
| rows not closed | connections leaking from the pool |
| t.Format("2006-01") == "2026-03" in SQL form | a half-open range |
