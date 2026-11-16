---
from: javascript
status: draft
concept: "Hexagonal and clean architecture"
from_way: "Route handlers that call Prisma, `fetch` and the message queue directly, with business rules in between."
native_way: "A `domain` folder of plain TypeScript with interfaces for what it needs, adapters for HTTP, the database and the queue, and a `main.ts` (or a NestJS module) that wires them; `dependency-cruiser` checks the rule."
sensei_tip: "TypeScript interfaces vanish at runtime, so a port costs nothing in the bundle. A plain object that matches the interface is enough as a fake in tests."
---

| JavaScript / TypeScript | Hexagonal idea |
| --- | --- |
| plain TypeScript modules in `domain/` | the core |
| `interface Orders` | driven port |
| Express or NestJS controllers, queue consumers | driving adapters |
| a Prisma or Drizzle repository | driven adapter |
| `main.ts`, a NestJS module with providers | composition root |
| `dependency-cruiser` forbidden rule | the dependency rule as a check |
