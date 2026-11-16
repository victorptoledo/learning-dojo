---
from: python
status: reviewed
concept: "Django apps as modules"
from_way: "One Django project where every app imports every other app's models."
native_way: "Django apps as modules with explicit service functions as their public API, import-linter to enforce the layers, and services extracted only when a force demands it."
sensei_tip: "import-linter's contracts (layers, independence) are the Python equivalent of ArchUnit: run them in CI."
---

| Python | Architecture |
| --- | --- |
| Django apps | modules of a modular monolith |
| `import-linter` | enforced boundaries |
| FastAPI service in front of mobile | a BFF |
| uv or Poetry workspaces | a Python monorepo |
