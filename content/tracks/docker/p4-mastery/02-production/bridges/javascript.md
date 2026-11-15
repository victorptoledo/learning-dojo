---
from: javascript
status: draft
concept: "Running services in production"
from_way: "pm2 cluster mode with reloads, .env files per environment, and deployments over SSH."
native_way: "One process per container, replicas instead of pm2 clusters, environment variables from the platform, and rolling updates driven by readiness."
sensei_tip: "Drop pm2 inside containers: one node process as PID 1 is easier to stop gracefully, limit and scale; let the orchestrator run more replicas."
---

| JavaScript | Production containers |
| --- | --- |
| `pm2 reload` | a rolling update |
| pm2 cluster mode | several replicas |
| `.env.production` | environment from the platform, secrets as files |
| `/health` route | liveness and readiness probes |
