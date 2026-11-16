---
from: javascript
status: draft
concept: "Security checks run in the delivery path"
from_way: "npm audit prints a wall of warnings that everyone scrolls past."
native_way: "Renovate or Dependabot pull requests, a scanner that fails only on exploitable high-severity issues, and npm ci with integrity hashes."
sensei_tip: "Install scripts run arbitrary code: npm ci --ignore-scripts in CI, allowing scripts only for the packages that need them."
---

| JavaScript world | DevSecOps |
| --- | --- |
| `npm audit` | SCA, triaged |
| `integrity` in the lock file | pinned supply chain |
| ESLint security plugins | SAST |
| a hijacked npm package | why provenance and pinning matter |
