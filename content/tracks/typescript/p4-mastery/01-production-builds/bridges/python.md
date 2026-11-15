---
from: python
status: draft
concept: "Shipping without a build step"
from_way: "Python runs the sources; mypy runs as a separate CI step; wheels package libraries; type hints never affect running code."
native_way: "Node 26 can run the .ts sources the same way, with tsc as the separate check; libraries are compiled to .js with .d.ts files."
sensei_tip: "Running .ts sources on Node is the Python model you know: interpreter runs, checker checks in CI. Libraries differ: npm consumers need compiled JavaScript and declaration files."
---

| Python | TypeScript |
| --- | --- |
| `python -m app` in the container | `node src/main.ts` |
| `uv build` → wheel | `tsc` → `dist/*.js` + `*.d.ts` |
| `mypy` step in CI | `tsc --noEmit` step in CI |
| `uv.lock` + `uv sync --frozen` | `package-lock.json` + `npm ci` |
| `pip-audit` | `npm audit` |
