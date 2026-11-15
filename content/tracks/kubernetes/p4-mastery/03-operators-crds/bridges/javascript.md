---
from: javascript
status: draft
concept: "Your own declarative APIs"
from_way: "A Node.js service with an endpoint that calls cloud SDKs to create resources, and a cron that fixes drift."
native_way: "The resource becomes a custom object; a controller watches it and converges the cloud to it continuously, with no separate drift job."
sensei_tip: "The @kubernetes/client-node package has informers; for anything beyond a prototype, controller-runtime in Go is where the ecosystem and the examples are."
---

| Node.js world | Kubernetes |
| --- | --- |
| a POST `/databases` endpoint | `kubectl apply` of a custom resource |
| a JSON Schema for the body | the CRD's `openAPIV3Schema` |
| a drift-fixing cron | the level-triggered reconcile |
| cascade delete in the database | owner references |
