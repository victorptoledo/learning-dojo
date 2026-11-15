---
from: python
status: draft
concept: "Running services in production"
from_way: "gunicorn under systemd behind nginx, with settings modules per environment and fab or Ansible deployments."
native_way: "The image carries gunicorn or uvicorn; the orchestrator replaces systemd and the deployment tool, routing traffic only to ready replicas and sending SIGTERM to old ones."
sensei_tip: "gunicorn's graceful_timeout should be shorter than the platform's grace period, and its worker count should follow the CPU limit, not the node's cores."
---

| Python | Production containers |
| --- | --- |
| `settings_prod.py` | environment variables in one image |
| systemd restarting gunicorn | the orchestrator and its probes |
| `fab deploy` | a pipeline pushing a digest and rolling it out |
| Alembic or Django migrations | expand and contract, run as a job |
