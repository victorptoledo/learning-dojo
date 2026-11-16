---
from: python
status: draft
concept: "A task definition is the Pod; a service keeps it running"
from_way: "Gunicorn in a container on a VM, with Celery workers in another container started by compose."
native_way: "ECS services on Fargate: one for the API behind an ALB, one for workers scaled by queue backlog."
sensei_tip: "Run Gunicorn with `--graceful-timeout` shorter than the task's `stopTimeout`, and log to stdout so the awslogs driver collects everything."
---

| Python world | AWS |
| --- | --- |
| `docker compose` services | ECS services |
| `.env` file | environment and `secrets` in the task definition |
| Celery autoscale | Service Auto Scaling on SQS backlog |
| `docker exec` | ECS Exec |
