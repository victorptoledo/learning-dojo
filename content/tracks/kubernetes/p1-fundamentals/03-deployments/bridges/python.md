---
from: python
status: draft
concept: "Rolling, revisioned releases"
from_way: "A Fabric or Ansible playbook updates servers one at a time with serial: 1 and restarts gunicorn."
native_way: "The Deployment controller performs the serial update itself, waits for each new Pod to be ready and keeps the previous version for a quick undo."
sensei_tip: "gunicorn --preload and a readiness endpoint that checks the database stop half-started workers from taking traffic during a rollout."
---

| Python world | Kubernetes |
| --- | --- |
| Ansible `serial: 1` | `maxUnavailable: 1` / `maxSurge` |
| a health check before re-adding a host | the readiness probe |
| rolling back to the previous release dir | `kubectl rollout undo` |
| a release tag in Git | the image tag in `spec.template` |
