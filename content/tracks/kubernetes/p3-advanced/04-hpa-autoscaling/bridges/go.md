---
from: go
status: draft
concept: "Automatic elasticity"
from_way: "A fixed pool of worker goroutines per host, and more hosts added by hand when queues grow."
native_way: "Replicas are the unit of scaling: the HPA or KEDA adds Pods, and a node autoscaler adds hosts."
sensei_tip: "Export a Prometheus gauge for in-flight work and scale on it with a Pods metric or KEDA's prometheus trigger: it reflects load better than CPU for I/O-bound Go services."
---

| Go world | Kubernetes |
| --- | --- |
| a worker pool size | replicas |
| a Prometheus gauge of queue depth | a custom or external metric |
| `GOMAXPROCS` tuning | CPU requests and the HPA target |
| adding hosts | Cluster Autoscaler or Karpenter |
