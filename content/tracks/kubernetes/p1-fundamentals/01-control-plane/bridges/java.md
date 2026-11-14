---
from: java
status: draft
concept: "The declarative reconciliation loop"
from_way: "Imperative scripts that check 'if it crashed, restart it' from a cron job."
native_way: "etcd stores the cluster's source of truth; controllers watch continuously and move the actual state towards the declared one."
sensei_tip: "etcd uses the Raft consensus algorithm and is the heart of the cluster, so it needs low-latency disks."
---
