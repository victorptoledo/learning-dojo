---
from: python
status: draft
concept: "An edge proxy configured by objects"
from_way: "An nginx server block in front of gunicorn, edited by hand on each server, with certbot for certificates."
native_way: "The proxy runs as a controller in the cluster; Ingress or HTTPRoute objects replace the server blocks, and cert-manager replaces certbot."
sensei_tip: "Tell Django or Flask it is behind a proxy (SECURE_PROXY_SSL_HEADER, or werkzeug's ProxyFix) so it knows requests arrived over HTTPS."
---

| Python world | Kubernetes |
| --- | --- |
| an nginx `server` block | an Ingress or HTTPRoute |
| `location /api { proxy_pass ...; }` | a path rule to a Service |
| certbot renewals | cert-manager |
| several sites on one nginx | host-based rules on one Gateway |
