---
from: python
status: draft
concept: "Automating checks around commits"
from_way: "Running ruff and black by hand, or only in CI."
native_way: "The pre-commit framework: a .pre-commit-config.yaml lists ruff, ruff-format, a large-file check and a secret scanner, and pre-commit install wires them into Git."
sensei_tip: "Run pre-commit run --all-files in CI as well. It uses the same config, so the laptop check and the CI check can never drift apart."
---

| Python tooling | Git hook |
| --- | --- |
| `.pre-commit-config.yaml` | `pre-commit` (and `commit-msg` with `--hook-type`) |
| `pre-commit install` | writes `.git/hooks/pre-commit` |
| `check-added-large-files` | blocks big blobs before they enter history |
| `pre-commit run --all-files` in CI | the check nobody can skip |
