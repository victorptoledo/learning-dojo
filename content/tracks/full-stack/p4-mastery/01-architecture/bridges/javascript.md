---
from: javascript
status: reviewed
concept: "Monorepos and BFFs in the JavaScript world"
from_way: "A front-end repository and an API repository that copy types between them, and a microservice per feature."
native_way: "A pnpm workspace with Nx or Turborepo, a shared contracts package, enforced module boundaries, and the meta-framework's server side as a BFF."
sensei_tip: "Nx's enforce-module-boundaries lint rule with tags (scope:checkout, type:ui) keeps a growing monorepo from turning into a tangle."
---

| JavaScript | Architecture |
| --- | --- |
| `pnpm-workspace.yaml` | a monorepo |
| `packages/contracts` | shared zod schemas or generated clients |
| Next.js route handlers, Remix loaders | a BFF next to the UI |
| `nx affected` / `turbo run --filter` | build and test only what changed |
