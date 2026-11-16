---
from: javascript
status: draft
concept: "Security as a program the teams trust"
from_way: "npm audit warnings ignored because there are hundreds of them."
native_way: "Prioritised dependency findings (reachable, exploited, exposed), Dependabot or Renovate with tests, CodeQL in pull requests, and push protection for secrets."
sensei_tip: "Most npm audit findings are in development-only or unreachable code; filter with `npm audit --omit=dev` and reachability-aware tools so the team sees the few that matter."
---

| JavaScript / TypeScript | Program practice |
| --- | --- |
| npm audit noise ignored | prioritised, reachable findings |
| manual dependency bumps | Renovate or Dependabot with CI |
| secrets found after the push | push protection |
| security says no | options with risk and cost |
| no champions | one champion per team |
