---
from: javascript
status: draft
concept: "Prepared, evidence-first, blameless"
from_way: "Rolling back the deploy and moving on, without checking what the compromised code did."
native_way: "Treat a malicious dependency or leaked token as an incident: scope what ran where, rotate every credential it could read, and search for indicators across environments."
sensei_tip: "After a compromised npm package, the lockfile and CI logs tell you which builds installed which version; every secret those builds could read is exposed, not only the npm token."
---

| JavaScript / TypeScript | Incident-ready practice |
| --- | --- |
| roll back and move on | scope what the bad version did |
| rotate the npm token only | rotate every secret in those builds |
| search one service | search indicators everywhere |
| blame the developer who updated | fix the process that allowed it |
| no contacts list | on-call, legal and vendor contacts ready |
