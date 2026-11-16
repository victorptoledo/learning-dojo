---
from: java
status: draft
concept: "Modules, quality gates and upgrade discipline"
from_way: "Large Java systems use modules or packages by domain, ArchUnit to enforce boundaries, SonarQube quality gates, and planned framework upgrades with OpenRewrite recipes."
native_way: "Large React apps use feature folders with public index files, lint rules for boundaries, CI quality gates, and React upgrades with codemods."
sensei_tip: "Codemods are React's OpenRewrite recipes, and import-boundary lint rules are its ArchUnit. Bring the same discipline you would to a Spring Boot major upgrade."
---

| Java | React |
| --- | --- |
| Package by feature | `features/<name>/` with `index.ts` |
| ArchUnit rules | ESLint import boundaries |
| SonarQube gate | CI with tsc, lint, tests, bundle budget |
| OpenRewrite recipes | React codemods |
| Shared UI library (Vaadin components) | Design system package + Storybook |
| APM with stack traces | Error monitoring with source maps |
