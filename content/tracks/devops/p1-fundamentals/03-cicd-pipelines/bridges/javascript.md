---
from: javascript
status: draft
concept: "The pipeline is a build file for the whole delivery"
from_way: "npm scripts chain lint, test and build on your machine."
native_way: "CI runs npm ci and the same scripts as parallel jobs, with the npm cache keyed by package-lock.json and the build passed on as an artefact."
sensei_tip: "Use npm ci, never npm install, in CI: it fails when the lock file and package.json disagree instead of silently changing them."
---

| JavaScript world | CI pipeline |
| --- | --- |
| `npm run lint && npm test` | parallel jobs |
| `package-lock.json` | the cache key |
| Turborepo affected builds | path filters |
| `dist/` | an uploaded artefact |
