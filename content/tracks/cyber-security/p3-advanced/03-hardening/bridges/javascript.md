---
from: javascript
status: draft
concept: "Run only what is needed, with only the rights it needs"
from_way: "node server.js as root on port 80, with devDependencies installed in production."
native_way: "An unprivileged user, a reverse proxy or capability for low ports, npm ci --omit=dev, and a minimal image."
sensei_tip: "Official Node images ship a `node` user: add `USER node` to the Dockerfile and listen on a high port; there is rarely a reason for Node to run as root."
---

| JavaScript / TypeScript | Hardened deployment |
| --- | --- |
| sudo node server.js | USER node, port 8080 behind a proxy |
| npm install in production | npm ci --omit=dev |
| node:latest | a pinned slim or distroless image |
| --inspect enabled | no debugger in production |
| all ports open | a default-deny firewall |
