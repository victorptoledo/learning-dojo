---
from: python
status: draft
concept: "Deploying Python services"
from_way: "An image with an interpreter and a virtual environment built by uv, gunicorn or uvicorn workers, pydantic-settings for configuration, pip-audit for vulnerabilities, and ruff and mypy in CI."
native_way: "A binary that needs no interpreter, one process using every core, configuration parsed into a struct at start-up, govulncheck, and go vet plus staticcheck in CI."
sensei_tip: "The final image does not need Python, a venv or even a shell: a distroless static image with one file is the normal Go deployment."
---

| Python | Go |
| --- | --- |
| `python:3.15-slim` + `uv sync` | `distroless/static` + one binary |
| `uvicorn --workers 4` | one process, goroutines on every core |
| `pydantic-settings` | a `LoadConfig()` that validates env vars |
| `pip-audit` | `govulncheck` |
| ruff + mypy | `go vet` + staticcheck or golangci-lint |
| `pyupgrade` | `go fix` |
| supporting several Python versions | the `go` line plus GODEBUG compatibility |
