---
from: javascript
status: draft
concept: "Role-based access, enforced by the platform"
from_way: "Middleware checks roles from a JWT on each route of your API."
native_way: "The API server does the same for the cluster: it authenticates a token, then RBAC checks the verb and resource for that identity."
sensei_tip: "A ServiceAccount token is a JWT: decode one (it is signed, not encrypted) to see its audience, expiry and the identity Kubernetes will use."
---

| Node.js world | Kubernetes |
| --- | --- |
| a JWT bearer token | a ServiceAccount or OIDC token |
| role-checking middleware | the RBAC authoriser |
| `roles: ['editor']` in the token | groups mapped to RoleBindings |
| an API key in `.env` | a projected, short-lived token |
