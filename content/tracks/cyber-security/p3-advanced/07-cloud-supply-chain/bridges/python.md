---
from: python
status: draft
concept: "Trust what you deploy and what deploys it"
from_way: "pip install -r requirements.txt with unpinned names, and AWS keys in environment variables of the CI."
native_way: "Locked dependencies with hashes (uv or pip-tools), pip-audit in CI, a private index without public fallback for internal names, and OIDC credentials."
sensei_tip: "`uv lock` records hashes for every artifact and `uv sync --locked` fails if the lock is stale; `pip install --require-hashes` gives the same guarantee with pip."
---

| Python | Supply chain practice |
| --- | --- |
| requests>=2 | a lockfile with hashes |
| --extra-index-url for private packages | one index that proxies PyPI |
| no vulnerability scan | pip-audit in CI |
| AWS_SECRET_ACCESS_KEY in CI | OIDC role assumption |
| FROM python:3 | a pinned digest |
