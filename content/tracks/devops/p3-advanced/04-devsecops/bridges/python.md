---
from: python
status: draft
concept: "Security checks run in the delivery path"
from_way: "pip install pulls whatever versions PyPI has today, and a .env file sits next to the code."
native_way: "Hashes in a lock file, pip-audit or OSV-Scanner in CI, Bandit or Semgrep for code, and secrets fetched from a manager at run time."
sensei_tip: "pip install --require-hashes refuses any package whose hash is not in the lock file: a cheap defence against a hijacked release."
---

| Python world | DevSecOps |
| --- | --- |
| `pip install` unpinned | a hashed lock file |
| `pip-audit` | SCA |
| Bandit | SAST |
| `.env` in the repo | a secrets manager |
