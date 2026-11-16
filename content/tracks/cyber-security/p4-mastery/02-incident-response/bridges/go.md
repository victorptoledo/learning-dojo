---
from: go
status: draft
concept: "Prepared, evidence-first, blameless"
from_way: "Killing the compromised container and letting the orchestrator start a fresh one."
native_way: "Keep the pod for analysis (remove it from the service, snapshot it), collect logs and network flows, rotate its service account and secrets, and redeploy from a verified image."
sensei_tip: "In Kubernetes, change the pod's labels so the Service and ReplicaSet stop managing it: traffic stops, a replacement starts, and the suspicious pod stays alive for forensics."
---

| Go | Incident-ready practice |
| --- | --- |
| kubectl delete pod | relabel and isolate the pod |
| logs only in the pod | logs shipped before the incident |
| same image redeployed | a rebuilt, verified image |
| service account kept | token and secrets rotated |
| no post-mortem | a blameless review with owners |
