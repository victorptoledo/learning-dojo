---
from: python
status: draft
concept: "Declared state instead of start scripts"
from_way: "A supervisor such as systemd or supervisord keeps gunicorn alive on one machine, and Fabric or Ansible scripts push new versions."
native_way: "You declare the image and the number of replicas; Kubernetes places them on nodes, restarts them and rolls out new versions."
sensei_tip: "One process per container: run gunicorn as the container's command and let Kubernetes do what supervisord did."
---

| Python world | Kubernetes |
| --- | --- |
| `gunicorn app:app` under supervisord | a container in a Pod, restarted by the kubelet |
| `fab deploy` | `kubectl apply` of a new image tag |
| several gunicorn hosts behind nginx | a Deployment with replicas behind a Service |
| a `.env` file | a ConfigMap and a Secret |
