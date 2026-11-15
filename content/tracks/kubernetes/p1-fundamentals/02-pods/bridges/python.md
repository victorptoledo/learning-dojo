---
from: python
status: draft
concept: "A Pod is a co-located group, scaled as one"
from_way: "On a VM, gunicorn, a log forwarder and a cron job all run side by side and share the disk."
native_way: "In a Pod, each runs as its own container; helpers share the network and volumes with the app, and one-off steps become init containers."
sensei_tip: "Run Alembic or Django migrations as an init container or a Job, never at import time in every replica; and make gunicorn exit cleanly on SIGTERM."
---

| Python world | Kubernetes |
| --- | --- |
| `manage.py migrate` before `runserver` | an init container |
| a log forwarder reading `/var/log/app` | a sidecar sharing an `emptyDir` |
| `signal.signal(SIGTERM, handler)` | graceful termination |
| a virtualenv per project | a namespace per team or app |
