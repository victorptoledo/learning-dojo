---
from: javascript
status: draft
concept: "One immutable artefact for every environment"
from_way: "`npm install` on the server, with node_modules that drift between machines."
native_way: "`npm ci` runs in a build stage from package-lock.json; the runtime stage copies only the built app and production dependencies."
sensei_tip: "Run node directly as the entrypoint, not through npm start, so SIGTERM reaches your process."
---

| JavaScript world | Containers |
| --- | --- |
| `node_modules` on the server | a cached layer from `npm ci` |
| `package-lock.json` | reproducible builds |
| `.npmignore` | `.dockerignore` |
| the npm registry | an image registry |
