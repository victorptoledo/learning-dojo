---
from: python
status: draft
concept: "Promote one artefact; let the environment pull its state"
from_way: "A deploy script SSHes into each environment and runs migrations and a restart."
native_way: "Each environment's desired state is a folder in Git; a controller in the cluster applies it, and migrations run as a Job or hook."
sensei_tip: "Alembic or Django migrations must stay backwards compatible across one release, or a revert of the image cannot be a rollback."
---

| Python world | GitOps |
| --- | --- |
| `deploy.sh prod` | a pull request to the prod config |
| a migration before restart | a Job or a sync hook |
| `.env.prod` | a sealed or external secret |
| a release tag in Git | an image tag in the config repo |
