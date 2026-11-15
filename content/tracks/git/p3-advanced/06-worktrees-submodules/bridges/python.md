---
from: python
status: draft
concept: "Sharing code between repositories"
from_way: "Copying a utils.py between projects, or pip install -e ../shared."
native_way: "Publish the shared code as a package (a private index or a Git URL pinned to a tag); use a submodule only when it cannot be packaged."
sensei_tip: "Each worktree needs its own virtual environment: python -m venv .venv inside it. Sharing one venv across worktrees mixes the editable installs of two branches."
---

| Python habit | Git option |
| --- | --- |
| copied `utils.py` | a package with versions |
| `pip install -e ../shared` | a submodule plus an editable install, or a monorepo |
| a second clone to test main | `git worktree add ../shop-main main` |
| `lib @ git+https://...@v1.2` | a Git dependency pinned to a tag |
