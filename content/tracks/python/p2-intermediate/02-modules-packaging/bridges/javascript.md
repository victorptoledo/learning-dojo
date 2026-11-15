---
from: javascript
status: draft
concept: "Modules and published packages"
from_way: "ES modules with import and export, index.js as a folder's entry, module code that runs once and is cached, package.json with bin and exports, and npm publish."
native_way: "Every .py file is a module, __init__.py is the folder's entry, modules run once and are cached in sys.modules, [project.scripts] replaces bin, and uv publish uploads to PyPI."
sensei_tip: "Python has no export keyword: every top-level name is importable. Mark the public API with __all__ and a leading underscore on everything internal."
---

| JavaScript | Python |
| --- | --- |
| `import { Invoice } from "./model.js"` | `from invoicer.model import Invoice` |
| `export function render()` | any top-level `def render():` (listed in `__all__`) |
| `index.js` | `__init__.py` |
| module cache | `sys.modules` |
| `"bin": { "invoicer": "cli.js" }` | `[project.scripts] invoicer = "invoicer.cli:main"` |
| `npm pack` / `npm publish` | `uv build` / `uv publish` |
| `npm install -g invoicer` | `uv tool install invoicer` |
| `"exports"` map | `__all__` and underscore names |
