---
from: python
status: draft
concept: "The architect's role and governance"
from_way: "A standards wiki listing approved libraries, enforced by a reviewer who must approve every new dependency."
native_way: "A cookiecutter or copier project template as the paved road, shared `import-linter` and Ruff configurations, ADRs with an advice forum, and architects who maintain the template and the checks."
sensei_tip: "A copier template can be re-applied to existing projects (`copier update`), so improvements to the paved road reach old services too, not only new ones."
---

| Python world | Governance idea |
| --- | --- |
| cookiecutter, copier templates | paved road |
| shared Ruff and `import-linter` configs | fitness functions as governance |
| `docs/adr` and an advisory forum | the advice process |
| a constraints file of approved versions | technology radar in code |
| notebooks and spikes | staying technical |
| Backstage templates | self-service paved roads |
