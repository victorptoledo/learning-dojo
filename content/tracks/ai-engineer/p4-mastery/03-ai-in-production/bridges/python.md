---
from: python
status: draft
concept: "Operate it for years, not weeks"
from_way: "Pinning dependencies, upgrading Python versions and running services with CI and on-call."
native_way: "Pinning model ids and prompt versions, running evals on every change, migrating models with shadow traffic and rollbacks, and documenting the system for governance."
sensei_tip: "Pin model ids like you pin packages in a lock file: an alias that silently points to a new model is the AI equivalent of an unpinned dependency."
---

| Python operations | AI feature operations |
| --- | --- |
| `uv.lock` pins | pinned model ids and prompt versions |
| upgrading Python 3.x | migrating to a new model |
| pytest in CI | evals in CI |
| feature flags | switching configurations |
| a README and ADRs | system documentation and decision records |
