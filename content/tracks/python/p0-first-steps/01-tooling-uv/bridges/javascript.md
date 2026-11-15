---
from: javascript
status: draft
concept: "Runtime manager, manifest and lock file"
from_way: "nvm for Node versions, package.json for dependencies and scripts, node_modules per project, package-lock.json, and npx for one-off tools."
native_way: "uv for Python versions, pyproject.toml as the manifest, .venv per project, uv.lock as the lock and uvx for one-off tools."
sensei_tip: "The mapping is almost one to one, and .venv is your node_modules: never commit it. pyproject.toml has no scripts section; run commands with uv run, or use a task runner."
---

| Node.js / npm | Python / uv |
| --- | --- |
| `nvm install 22` | `uv python install 3.15` |
| `package.json` | `pyproject.toml` |
| `devDependencies` | `[dependency-groups] dev` |
| `node_modules/` | `.venv/` |
| `package-lock.json` | `uv.lock` |
| `npm ci` | `uv sync --locked` |
| `npx eslint .` | `uvx ruff check .` |
| `if (require.main === module)` | `if __name__ == "__main__":` |
