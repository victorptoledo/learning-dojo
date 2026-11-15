---
from: javascript
status: draft
concept: "A Pod is a co-located group, scaled as one"
from_way: "One VM runs the Node.js server, an nginx in front of it and a script that runs migrations before starting."
native_way: "A Pod holds the Node.js container, an optional proxy sidecar and an init container for migrations; they share localhost and volumes."
sensei_tip: "Node.js as PID 1 ignores SIGTERM unless you handle it: call server.close() on process.on('SIGTERM') and exit when connections drain."
---

| Node.js world | Kubernetes |
| --- | --- |
| `npm run migrate && npm start` | an init container, then the app container |
| nginx on the same host | a sidecar on `localhost` |
| `process.on('SIGTERM', ...)` | graceful termination |
| `package.json` name and version | the recommended `app.kubernetes.io/*` labels |
