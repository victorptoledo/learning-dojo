---
from: python
status: draft
concept: "Declare the service, controllers keep it running"
from_way: "supervisord keeps gunicorn alive on each VM, and a load balancer health check removes broken VMs."
native_way: "The kubelet restarts containers, the Deployment keeps the replica count, and the Service routes only to Pods whose readiness probe passes."
sensei_tip: "Give gunicorn a worker count that fits the CPU request, not the node's cores; os.cpu_count() sees the whole machine."
---

| Python world | Kubernetes |
| --- | --- |
| supervisord | the kubelet and restartPolicy |
| a health check view | a readiness probe |
| `settings.py` per env | a ConfigMap |
| `.env` with passwords | a Secret (or an external secret) |
