---
from: javascript
status: draft
concept: "Modular monolith"
from_way: "One Node.js application where any file imports any other and every module queries every table through one Prisma client."
native_way: "Workspace packages or NestJS modules per capability, each exporting a small interface, Nx `enforce-module-boundaries` or `dependency-cruiser` rules, an in-process event emitter between modules, and separate schemas."
sensei_tip: "In an Nx or Turborepo monorepo, tag each library with its module and forbid imports across tags except to the `api` library: the build then enforces your boundaries."
---

| JavaScript / TypeScript | Modular monolith idea |
| --- | --- |
| a workspace package or NestJS module per capability | modules |
| `exports` in `package.json`, NestJS `exports` | published interface |
| Nx tags and `enforce-module-boundaries` | boundaries as a check |
| `EventEmitter`, NestJS `@OnEvent` | in-process events |
| one Prisma schema file per module | data ownership |
| deep imports into another package's `src` | a leak to remove |
