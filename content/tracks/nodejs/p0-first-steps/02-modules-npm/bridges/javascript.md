---
from: javascript
status: draft
concept: "ES modules on the server, and the CommonJS legacy"
from_way: "In the browser you import ES modules by URL or let a bundler resolve bare names, and package.json is mostly the bundler's business."
native_way: "Node.js resolves bare names from node_modules itself, decides ESM vs CommonJS from the extension and the type field, and reads exports to find a package's entry points."
sensei_tip: "Most Node.js confusion comes from mixing the two module systems. Choose type module, use explicit extensions and the node: prefix, and treat require as something you read in older code."
---

| Browser / bundler | Node.js |
| --- | --- |
| `import x from "./x.js"` | The same (extension required) |
| Bundler resolves `"lodash"` | Node.js finds `node_modules/lodash` |
| Everything is ESM | ESM or CommonJS per file |
| `import.meta.url` | `import.meta.url`, `.dirname`, `.filename` |
| Import maps | `imports` field (`#internal/...`) |
