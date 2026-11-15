---
from: javascript
status: draft
concept: "Least privilege for the process and the network"
from_way: "The official node image runs as root unless you switch to the node user, and the app can call anything on the internet."
native_way: "The Pod runs as a non-root UID with capabilities dropped, and egress NetworkPolicies limit outgoing calls to known dependencies."
sensei_tip: "The node images ship a node user with UID 1000: set runAsUser: 1000 and runAsNonRoot: true, and allow egress only to the APIs you call, plus DNS."
---

| Node.js world | Kubernetes |
| --- | --- |
| `USER node` in the Dockerfile | `runAsUser: 1000`, `runAsNonRoot: true` |
| a CSP for the browser | an egress NetworkPolicy for the server |
| `npm audit` | admission policies and image scanning |
| binding to port 80 as root | port 8080, or `NET_BIND_SERVICE` only |
