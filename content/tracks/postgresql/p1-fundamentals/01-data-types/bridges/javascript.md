---
from: javascript
status: reviewed
concept: "Types that carry rules"
from_way: "TypeScript union types such as 'new' | 'paid', Zod schemas, arrays and Date objects."
native_way: "Enums, domains, arrays and ranges in the database, with exclusion constraints that hold for every client."
sensei_tip: "A JavaScript Date is an instant, like timestamptz; node-postgres returns timestamp without time zone in the server's local zone, which is a classic bug. Use timestamptz."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| type Status = 'new' or 'paid' | ENUM, or text with CHECK |
| z.string().email() | CREATE DOMAIN email ... |
| string[] | text[] |
| Date | timestamptz |
| { start, end } | tstzrange |
| a check before insert | EXCLUDE USING gist |
