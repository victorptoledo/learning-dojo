---
from: python
status: draft
concept: "Governance, style guides and review"
from_way: "Docstrings and a wiki page as the only guidance."
native_way: "A shared ruleset for Spectral or Vacuum run in CI on the exported schema, shared Pydantic base models for problems and pages, and review of the schema diff."
sensei_tip: "Ship the shared `Problem` and `Page` models as an internal package; consistency is far easier when the right shape is an import away."
---

| Python | Governance concept |
| --- | --- |
| Spectral / Vacuum in CI | automated style guide |
| internal package with base models | consistency by reuse |
| schema diff in the pull request | design review |
| `ruff` custom rules | code conventions |
| catalog-info.yaml | API catalogue |
