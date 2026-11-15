---
from: javascript
status: draft
concept: "Version control in a Node.js project"
from_way: "Copying the project with node_modules/, or sharing it through a cloud drive."
native_way: "Commit package.json and the lock file, ignore node_modules/ and build output such as dist/."
sensei_tip: "Always commit package-lock.json (or pnpm-lock.yaml, yarn.lock). Without it two clones can install different dependency versions from the same package.json."
---

| Node habit | With Git |
| --- | --- |
| `node_modules/` in the zip | ignored; `npm ci` rebuilds it from the lock file |
| `dist/`, `build/` | ignored; CI builds them |
| `.env` with secrets | ignored; commit an `.env.example` instead |
| Prettier rewrites CRLF | `* text=auto eol=lf` in `.gitattributes` |
