---
from: java
status: draft
concept: "Distributed orchestration"
from_way: "Installing application servers by hand on several virtual machines."
native_way: "Declare the desired state in YAML manifests; the control plane keeps the right number of replicas running."
sensei_tip: "Never create bare pods: use a `Deployment` for the lifecycle and a `Service` to hide the pods' changing IPs."
---
