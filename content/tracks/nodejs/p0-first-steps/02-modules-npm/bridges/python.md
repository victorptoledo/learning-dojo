---
from: python
status: draft
concept: "Packages, imports and reproducible installs"
from_way: "import searches sys.path, dependencies live in pyproject.toml, a virtualenv isolates them, and uv.lock or requirements.txt pins versions."
native_way: "import resolves files by path and packages from node_modules up the folder tree; node_modules is the per-project environment, and package-lock.json pins versions."
sensei_tip: "node_modules is a virtualenv that is always on: no activation, one per project. The equivalent of uv sync --frozen is npm ci."
---

| Python | Node.js |
| --- | --- |
| `from pkg.mod import f` | `import { f } from "pkg/mod"` |
| `pyproject.toml` | `package.json` |
| `.venv/` | `node_modules/` |
| `uv.lock` | `package-lock.json` |
| `uv sync --frozen` | `npm ci` |
| `[project.scripts]` | `bin` and `scripts` |
