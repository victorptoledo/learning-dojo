---
from: javascript
status: draft
concept: "Naming and representation conventions"
from_way: "`Number` for money and `Date.toString()` in responses."
native_way: "Integer cents (or decimal strings), `date.toISOString()`, string-literal unions for enums, and an envelope object for lists."
sensei_tip: "`0.1 + 0.2 !== 0.3` in JavaScript; and `JSON.parse` loses precision above 2^53, so large ids must be strings."
---

| JavaScript / TypeScript | Convention |
| --- | --- |
| `new Date().toISOString()` | RFC 3339 timestamp |
| `amountCents: number` (integer) | money |
| `type Status = 'PENDING' \| 'PAID'` | enum (a string-literal union) |
| ids as `string` | no precision loss |
| `{ items, nextCursor }` | collection envelope |
