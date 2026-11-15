---
from: javascript
status: draft
concept: "Least privilege and a minimal attack surface"
from_way: "Running node under pm2 as your own user, with npm and build tools installed on the server."
native_way: "Official Node images include a node user: copy the app as root, then USER node; build tools stay in an earlier stage."
sensei_tip: "A health check can use node itself: HEALTHCHECK CMD [\"node\", \"-e\", \"fetch('http://localhost:3000/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))\"]."
---

| JavaScript | Image hardening |
| --- | --- |
| running as your login user | `USER node` (UID 1000) or a numeric user |
| npm on the server | only in the build stage |
| `/health` route | the health check target |
| `npm audit` | image scanning (Phase 3) |
