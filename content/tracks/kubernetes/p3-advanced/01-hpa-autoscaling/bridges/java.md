---
from: java
status: draft
concept: "Automatic elasticity"
from_way: "Provisioning servers for the yearly traffic peak and paying for them all year."
native_way: "The HPA raises and lowers the number of replicas as real demand changes."
sensei_tip: "Use KEDA to scale on queue length (SQS, Kafka) instead of generic CPU metrics."
---
