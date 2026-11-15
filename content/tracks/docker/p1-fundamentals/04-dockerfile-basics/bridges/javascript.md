---
from: javascript
status: draft
concept: "A declarative build recipe"
from_way: "package.json scripts (build, start) describe how to build and run, and .npmignore decides what gets published."
native_way: "The Dockerfile runs the build (RUN npm ci, RUN npm run build) and declares how the result starts; .dockerignore decides what the builder may see."
sensei_tip: "Always put node_modules in .dockerignore: a folder installed on Windows contains native modules for the wrong OS, and it is huge."
---

| JavaScript | Dockerfile |
| --- | --- |
| `"start": "node dist/server.js"` | `CMD ["node", "dist/server.js"]` |
| `"bin"` for a CLI | `ENTRYPOINT ["node", "/app/cli.js"]` |
| `.npmignore` | `.dockerignore` |
| `process.env.PORT ?? 3000` | `ENV PORT=3000` |
