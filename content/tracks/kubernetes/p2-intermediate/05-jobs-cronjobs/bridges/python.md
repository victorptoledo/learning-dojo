---
from: python
status: draft
concept: "Batch and scheduled work"
from_way: "crontab lines on a server, or Celery beat scheduling tasks for workers."
native_way: "A CronJob is the cluster's crontab: each run is a Job with its own Pod, retries, deadline and logs."
sensei_tip: "A Django management command makes a clean Job entrypoint: args: [\"python\", \"manage.py\", \"send_reports\"]."
---

| Python world | Kubernetes |
| --- | --- |
| a crontab line | a CronJob |
| `celery beat` | a CronJob, or keep Celery for many small tasks |
| a one-off `python migrate.py` | a Job |
| a log agent on every host | a DaemonSet |
