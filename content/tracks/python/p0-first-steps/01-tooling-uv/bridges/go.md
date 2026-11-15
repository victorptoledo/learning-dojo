---
from: go
status: draft
concept: "Toolchain, module file and checksums"
from_way: "One go toolchain, go.mod with the required versions, go.sum with checksums, and a module cache shared by every project."
native_way: "uv installs the interpreter, pyproject.toml holds ranges, uv.lock holds exact versions and hashes, and each project has its own .venv."
sensei_tip: "go.mod plus go.sum is close to pyproject.toml plus uv.lock. Go picks the minimal versions that satisfy go.mod; uv picks the newest versions that fit the ranges, so the lock is what keeps builds repeatable."
---

| Go | Python / uv |
| --- | --- |
| `go version`, the `toolchain` directive | `uv python install 3.15`, `.python-version` |
| `go mod init` | `uv init` |
| `go get github.com/x/y` | `uv add x` |
| `go.mod` | `pyproject.toml` |
| `go.sum` | `uv.lock` |
| `go run .` / `go test ./...` | `uv run python main.py` / `uv run pytest` |
| `go run pkg@version` | `uvx tool@version` |
