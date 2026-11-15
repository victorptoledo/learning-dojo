---
from: javascript
status: draft
concept: "Sharing code between repositories"
from_way: "npm link between two local folders, which breaks as soon as someone else clones the project."
native_way: "A workspace monorepo (npm, pnpm or yarn workspaces) or a published package; submodules are rarely the right tool in the Node world."
sensei_tip: "A new worktree has no node_modules (it is ignored), so run npm ci in it first. pnpm's shared store makes that nearly free."
---

| Node habit | Git option |
| --- | --- |
| `npm link ../shared` | workspaces in a monorepo |
| a private package on a registry | a package (preferred) |
| `"lib": "github:acme/lib#v1.2.0"` | a Git dependency pinned to a tag |
| a second clone for a hotfix | `git worktree add -b hotfix/x ../shop-hotfix main` |
