---
from: java
status: draft
concept: "The declarative reconciliation loop"
from_way: "Imperative scripts or a cron job that checks 'if it crashed, restart it', plus a runbook for the cases the script misses."
native_way: "etcd stores the cluster's source of truth; controllers watch continuously and move the actual state towards the declared one."
sensei_tip: "etcd uses the Raft consensus algorithm and is the heart of the cluster, so it needs low-latency disks. The Java client fabric8 offers informers, the same watch-and-cache pattern controllers use."
---

| Java world | Kubernetes |
| --- | --- |
| a ZooKeeper ensemble holding cluster state | etcd (also consensus-based, 3 or 5 members) |
| a scheduled health-check job | a controller's reconcile loop |
| JMX listeners for change events | API watches |
| a central admin server | the kube-apiserver |
