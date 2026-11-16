---
from: javascript
status: draft
concept: "Governance, style guides and review"
from_way: "ESLint for code and nothing for the API surface."
native_way: "Redocly or Spectral rules versioned in a shared package, `openapi-typescript` builds that fail on contract drift, and review of the YAML diff."
sensei_tip: "Publish the ruleset as an npm package (`@company/api-ruleset`) and `extends` it in every repo: one place to change a rule for all teams."
---

| JavaScript / TypeScript | Governance concept |
| --- | --- |
| `@company/api-ruleset` (Spectral) | shared style guide |
| Redocly CLI `lint` | automated checks |
| generated types failing the build | drift detection |
| YAML diff in the pull request | design review |
| Backstage | API catalogue |
