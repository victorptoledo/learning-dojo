---
from: python
status: draft
concept: "Health endpoints and sized containers"
from_way: "A /ping route for the load balancer, and workers that grow until the machine swaps."
native_way: "Readiness and liveness probes hit separate routes, and memory limits stop a leaking worker before it hurts its neighbours."
sensei_tip: "Set gunicorn --max-requests to recycle leaking workers, and size --workers from the CPU request, not from os.cpu_count(), which shows the whole node."
---

| Python world | Kubernetes |
| --- | --- |
| a Flask `/ping` route | a liveness probe |
| a `/ready` route that checks the database | a readiness probe |
| `--workers $(nproc)` | workers from the CPU request |
| a growing worker process | a memory limit and an OOM kill |
