---
from: python
status: draft
concept: "Projects, dependencies and running code"
from_way: "pyproject.toml, uv or pip, a virtual environment per project, an interpreter installed wherever the code runs, and ruff for formatting and linting."
native_way: "go.mod describes the module, there is no virtual environment (the module cache is shared and versioned), and go build turns the program into a binary that needs nothing installed."
sensei_tip: "Notice what disappears: no activate step, no interpreter on the server, no requirements file drift. go.mod plus go.sum are the whole story."
---

| Python | Go |
| --- | --- |
| `uv init` | `go mod init example.com/app` |
| `pyproject.toml` + `uv.lock` | `go.mod` + `go.sum` |
| `uv add httpx` | `go get github.com/x/y@v1.2.3` |
| `.venv/` per project | one module cache for every project |
| `uv run app.py` | `go run .` |
| `ruff format` | `gofmt -w .` |
| `_private` by convention | lower case names are enforced as package-private |
| `python -m pytest` | `go test ./...` |
