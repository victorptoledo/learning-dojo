---
from: python
status: draft
concept: "Build pipelines"
from_way: "pip or uv for dependencies; Python ships source, so there is rarely a build step."
native_way: "Frontend code is built: resolved, transformed, bundled and minified, because every byte is downloaded by every user."
sensei_tip: "Think of the bundle as a wheel for the browser, rebuilt on each release, with a hash in its name so caches never serve the old one."
---

| Python | In the browser |
| --- | --- |
| `pyproject.toml` dependencies | `package.json` dependencies |
| `python -m build` | `vite build` |
| Importing a module lazily inside a function | Dynamic `import()` |
| `.env` with python-dotenv | `import.meta.env`: public in the bundle |
