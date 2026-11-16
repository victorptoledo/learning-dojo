---
from: javascript
status: draft
concept: "The architect's role and governance"
from_way: "Each team picks its own framework, bundler and state library, and an architect sends a monthly email about consistency."
native_way: "A paved road (a shared ESLint config, a project generator in an Nx or Turborepo workspace), dependency-cruiser rules as a package, ADRs and an advice forum, and Renovate keeping versions converged."
sensei_tip: "A shared ESLint and TypeScript config published as a package changes behaviour in every repository with one version bump. It is governance teams can see, run locally and propose changes to."
---

| JavaScript / TypeScript world | Governance idea |
| --- | --- |
| a shared ESLint and TS config package | paved road |
| Nx generators, `create-*` templates | self-service paved roads |
| `dependency-cruiser` rules package | fitness functions as governance |
| `docs/adr` and an advisory forum | the advice process |
| Renovate with grouped updates | convergence on supported versions |
| prototypes and spikes | staying technical |
