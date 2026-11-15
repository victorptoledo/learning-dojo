---
from: javascript
status: draft
concept: "Separate build and runtime environments"
from_way: "npm run build turns TypeScript and bundles into dist/, but devDependencies, sources and caches stay in the folder."
native_way: "Build in one stage, then start a fresh runtime stage that installs production dependencies only and copies dist/."
sensei_tip: "For a static front-end, the runtime stage does not even need Node: copy dist/ into an nginx or caddy image."
---

| JavaScript | Multi-stage |
| --- | --- |
| `npm run build` | in the build stage |
| `devDependencies` | never in the runtime stage |
| `dist/` | `COPY --from=build /app/dist ./dist` |
| a static SPA | `FROM nginx:1.29-alpine` serving the copied files |
