---
from: javascript
status: draft
concept: "Describing a multi-service environment"
from_way: "npm scripts with concurrently start the front-end, the API and a mock server; dotenv loads .env."
native_way: "Each part becomes a service in compose.yaml with its own image, and docker compose watch syncs or rebuilds them as you edit."
sensei_tip: "Use develop.watch with action sync for source files and rebuild for package.json, instead of bind-mounting node_modules from Windows."
---

| JavaScript | Compose |
| --- | --- |
| `concurrently "npm:dev:*"` | several services in one `compose.yaml` |
| `dotenv` | `.env` interpolation and `env_file:` |
| `nodemon` with a bind mount | `docker compose watch` |
| `wait-on tcp:5432` | `depends_on` with `condition: service_healthy` |
