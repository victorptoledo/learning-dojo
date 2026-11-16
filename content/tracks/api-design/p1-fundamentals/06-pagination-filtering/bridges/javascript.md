---
from: javascript
status: draft
concept: "Pagination, filtering and sorting"
from_way: "`skip`/`take` (Prisma) or `OFFSET` with page numbers for every list."
native_way: "Prisma's `cursor` + `take` + `skip: 1`, or a keyset `WHERE (created_at, id) < ($1, $2)` with an index; cursors as base64url strings."
sensei_tip: "`Buffer.from(json).toString('base64url')` gives URL-safe cursors without padding (Node 16+)."
---

| JavaScript / TypeScript | Collection concept |
| --- | --- |
| `skip` / `take` | offset |
| Prisma `cursor: { id }` | keyset cursor |
| `orderBy: [{ createdAt: 'desc' }, { id: 'desc' }]` | tie-breaker |
| `z.coerce.number().max(100)` | maximum limit |
| `base64url` | opaque cursor |
