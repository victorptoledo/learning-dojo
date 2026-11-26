---
from: python
status: draft
concept: "Workspaces and local packages"
from_way: "uv workspaces with members in one lock file, editable installs of local packages, one mypy run over everything or per package."
native_way: "npm or pnpm workspaces link local packages, and project references let tsc check each package separately and incrementally."
sensei_tip: "uv workspaces and npm workspaces solve the same problem. Python has no project references: that is the part TypeScript adds, and it makes the type check scale."
---

| Python (uv) | TypeScript |
| --- | --- |
| `[tool.uv.workspace] members = ["packages/*"]` | `"workspaces": ["packages/*"]` |
| `acme-domain = { workspace = true }` | `"@acme/domain": "workspace:*"` (pnpm) |
| Editable install | Symlink in `node_modules` |
| `mypy` per package | `tsc -b` with references |
| `uv.lock` for all members | One lock file at the root |
