---
from: javascript
status: draft
concept: "The API as a contract"
from_way: "`res.json(await db.order.findUnique(...))`: the ORM row is the API."
native_way: "Typed response DTOs (TypeScript types or zod schemas) mapped from the data layer, ideally generated from or checked against an OpenAPI document."
sensei_tip: "`res.json(row)` exposes columns added next month too. A zod schema with `.parse()` on the way out makes the contract enforceable."
---

| JavaScript / TypeScript | API concept |
| --- | --- |
| Prisma / TypeORM entity | implementation |
| `z.object({...})` response schema | the representation |
| `openapi.yaml` + generated types | design-first contract |
| `JSON.stringify(anything)` | Hyrum's law in waiting |
