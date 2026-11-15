---
from: javascript
status: draft
concept: "Large repositories"
from_way: "A workspace monorepo where everyone installs and builds every package."
native_way: "Sparse checkout of the packages you work on, and a task runner (Nx, Turborepo) that only builds and tests what changed."
sensei_tip: "Turborepo and Nx compute 'affected' packages from git diff against the base branch, so CI needs that base: fetch it explicitly when you clone shallow."
---

| Node situation | Git technique |
| --- | --- |
| 80 packages in `packages/` | `git sparse-checkout set packages/checkout packages/ui` |
| `nx affected` in CI | needs history back to the merge base (`fetch-depth: 0` or fetch main) |
| committed `dist/` or videos | ignore, or LFS for assets that must be versioned |
| slow `git status` | `core.fsmonitor true` |
