---
from: javascript
status: draft
concept: "Automating checks around commits"
from_way: "npm run lint before committing, when you remember."
native_way: "Husky sets core.hooksPath to .husky/ during npm install; its pre-commit runs lint-staged and its commit-msg runs commitlint."
sensei_tip: "Husky installs through the prepare script, so a clone gets the hooks on its first npm install. In CI set HUSKY=0 to skip installing them."
---

| Node tooling | Git hook |
| --- | --- |
| `.husky/pre-commit` → `npx lint-staged` | `pre-commit`, on staged files only |
| `.husky/commit-msg` → `npx commitlint --edit $1` | `commit-msg` |
| `"prepare": "husky"` | sets `core.hooksPath` |
| `HUSKY=0` | no hooks in CI |
