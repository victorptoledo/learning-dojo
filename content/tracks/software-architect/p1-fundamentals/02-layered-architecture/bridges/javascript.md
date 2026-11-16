---
from: javascript
status: draft
concept: "Layered architecture"
from_way: "`routes/`, `controllers/`, `services/` and `models/` folders in an Express app, with every file importing any other."
native_way: "Feature folders (`orders/`, `payments/`) with the route, service and data access inside and an `index.ts` as the public interface; NestJS modules export only what other modules may use."
sensei_tip: "NestJS modules already declare `providers` and `exports`: a provider that is not exported is package-private. Use that instead of one giant `SharedModule`."
---

| JavaScript / TypeScript | Layering idea |
| --- | --- |
| Express routes, NestJS controllers | presentation layer |
| service classes or functions | application layer |
| Prisma, TypeORM, Drizzle | persistence layer |
| `dependency-cruiser` forbidden rules | closed layers as a check |
| NestJS `@Module({ exports })` | a feature's public interface |
| a service that only calls `prisma.x.create` | sinkhole |
