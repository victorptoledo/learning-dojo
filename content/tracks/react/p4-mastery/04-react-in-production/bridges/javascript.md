---
from: javascript
status: draft
concept: "From a project to a platform"
from_way: "A JavaScript project grows by adding files to components/ and utils/, with ad-hoc styles and occasional tests."
native_way: "A production React codebase has feature boundaries, a design system, strict types and lint rules, a test pyramid in CI, budgets and monitoring."
sensei_tip: "The biggest wins are rarely clever React tricks: a design system that is accessible by default, error monitoring with source maps, and CI that blocks regressions."
---

| Ad-hoc JavaScript | Production React |
| --- | --- |
| `components/` with 400 files | `features/*` + `shared/ui` |
| Copy-pasted buttons | Design system `Button` |
| `console.error` in production | Error boundaries + monitoring |
| One big bundle | Route-level code splitting, budgets |
| `innerHTML` for rich text | Sanitised HTML or markdown to elements |
| Upgrade when forced | Planned upgrades with codemods |
