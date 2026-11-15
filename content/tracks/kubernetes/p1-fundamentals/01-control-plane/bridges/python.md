---
from: python
status: draft
concept: "The declarative reconciliation loop"
from_way: "A Celery beat task runs every minute, checks that workers are alive and restarts them; if it misses a run, nothing catches up."
native_way: "Controllers watch the API and reconcile from the current state, so a missed event changes nothing: the next look closes the gap."
sensei_tip: "The official kubernetes Python client has a watch API, and kopf is a framework for writing operators in Python on the same loop."
---

| Python world | Kubernetes |
| --- | --- |
| a periodic check-and-fix task | a level-triggered controller |
| a Redis key holding "how many workers" | `spec.replicas` stored in etcd |
| a Flask admin API | the kube-apiserver |
| supervisord on one host | the kubelet on each node |
