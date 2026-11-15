---
from: go
status: draft
concept: "Storage that outlives the process"
from_way: "An embedded BoltDB or Badger file next to the binary on one host."
native_way: "An embedded store needs a StatefulSet with one PVC per replica, so each Pod keeps its own file across restarts and rescheduling."
sensei_tip: "Raft-based Go systems (etcd, Consul, NATS JetStream) are the textbook StatefulSet case: stable names for peers and a volume per member."
---

| Go world | Kubernetes |
| --- | --- |
| `bolt.Open("data.db")` | a StatefulSet with `volumeClaimTemplates` |
| peers listed by hostname | `name-0.service` DNS records |
| `os.MkdirTemp` | an `emptyDir` |
| a Raft cluster of 3 | a StatefulSet with 3 replicas and a PDB |
