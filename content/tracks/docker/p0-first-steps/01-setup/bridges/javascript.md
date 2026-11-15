---
from: javascript
status: draft
concept: "Packaging an application with its runtime"
from_way: "package.json and the lock file pin your dependencies, but the Node.js version, native modules and the OS still differ between machines."
native_way: "An image pins the Node.js version and the installed node_modules for one OS, so native modules built in the image run in the image."
sensei_tip: "Use node:24-alpine or a slim tag, and install with npm ci from the lock file; never copy a node_modules folder built on Windows into a Linux image."
---

| JavaScript / TypeScript | Docker |
| --- | --- |
| `nvm use 24` | `FROM node:24-alpine` |
| `npm ci` | a build step in the Dockerfile |
| `node dist/server.js` | `CMD ["node", "dist/server.js"]` |
| npm registry | container registry for images |
