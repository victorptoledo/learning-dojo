---
from: go
status: reviewed
concept: "Types that carry rules"
from_way: "typed string constants, struct validation tags, []string slices and time.Time."
native_way: "Enums, domains, arrays and ranges in the database, with exclusion constraints that hold for every client."
sensei_tip: "pgx scans text[] into []string and tstzrange into pgtype.Range[pgtype.Timestamptz]; scan timestamptz into time.Time and keep it in UTC."
---

| Go | PostgreSQL |
| --- | --- |
| type Status string; const Paid Status = "paid" | ENUM, or text with CHECK |
| validate:"email" | CREATE DOMAIN email ... |
| []string | text[] |
| time.Time | timestamptz |
| pgtype.Range[pgtype.Timestamptz] | tstzrange |
| a mutex around "is it free?" | EXCLUDE USING gist |
