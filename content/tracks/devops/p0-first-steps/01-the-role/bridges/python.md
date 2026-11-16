---
from: python
status: draft
concept: "Developers own their service in production"
from_way: "You push a Django app and someone else sets up gunicorn, nginx, the virtualenv and the cron jobs on the servers."
native_way: "The repository holds the Dockerfile, the pipeline and the infrastructure code; your team ships and operates the service."
sensei_tip: "Python is also the glue language of operations: boto3, Ansible modules and most automation scripts. Your scripting is a head start."
---

| Python world | DevOps |
| --- | --- |
| `pip install` on the server | a pinned image built in CI |
| `fab deploy` from a laptop | a deploy job in the pipeline |
| a cron job on one box | a scheduled job defined as code |
| print debugging | structured logs and traces |
