---
from: javascript
status: draft
concept: "Configuration outside the artifact"
from_way: "A .env file loaded by dotenv, and build-time variables baked into the bundle."
native_way: "Server-side config comes from ConfigMaps and Secrets at run time, so one image serves every environment."
sensei_tip: "Front-end bundles bake variables in at build time; serve a small config.json from a ConfigMap instead, so the same static image works in every environment."
---

| Node.js world | Kubernetes |
| --- | --- |
| `require('dotenv').config()` | `envFrom` |
| `process.env.API_KEY` | `secretKeyRef` |
| `config/production.json` | a ConfigMap mounted as a file |
| Vercel environment variables | ConfigMaps and Secrets per namespace |
