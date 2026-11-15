---
from: java
status: draft
concept: "Automatic elasticity"
from_way: "Provisioning servers for the yearly traffic peak and paying for them all year."
native_way: "The HPA raises and lowers the number of replicas as real demand changes."
sensei_tip: "Use KEDA to scale on queue length (SQS, Kafka) instead of generic CPU metrics; and remember JVM warm-up: scale up early, with a target around 60-70 % CPU."
---

| Java world | Kubernetes |
| --- | --- |
| an AWS Auto Scaling group for app servers | an HPA for a Deployment |
| a Kafka consumer group sized by hand | a KEDA ScaledObject on consumer lag |
| `-Xmx` tuning by load tests | VPA recommendations for requests |
| buying servers for the peak | Cluster Autoscaler or Karpenter |
