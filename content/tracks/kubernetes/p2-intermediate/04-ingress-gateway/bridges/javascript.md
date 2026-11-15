---
from: javascript
status: draft
concept: "An edge proxy configured by objects"
from_way: "An Express app proxies /api with http-proxy-middleware, or a PaaS router maps domains to apps."
native_way: "The cluster's edge proxy routes hosts and paths to Services, declared in Ingress or HTTPRoute objects."
sensei_tip: "Set app.set('trust proxy', 1) in Express behind the proxy, or req.ip and req.secure report the proxy instead of the client."
---

| Node.js world | Kubernetes |
| --- | --- |
| `http-proxy-middleware` | an HTTPRoute backend |
| Vercel rewrites | HTTPRoute filters (URLRewrite) |
| a custom domain in a dashboard | `hostnames` on a route and a Gateway listener |
| preview deployments by URL | host or header matches to separate Services |
