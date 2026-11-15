---
from: javascript
status: draft
concept: "Incremental builds and dependency caching"
from_way: "npm keeps a global cache and only reinstalls when package-lock.json changes."
native_way: "Docker only knows its layers: COPY package.json package-lock.json, RUN npm ci, then COPY the rest, so code edits never reinstall."
sensei_tip: "npm ci --omit=dev in the final image keeps devDependencies (TypeScript, test runners) out; build in an earlier stage."
---

| JavaScript | Dockerfile |
| --- | --- |
| `package-lock.json` changed | the install layer is rebuilt |
| `npm ci` | reproducible install from the lock |
| `npm prune --omit=dev` | `npm ci --omit=dev` in the runtime stage |
| ESLint | hadolint |
