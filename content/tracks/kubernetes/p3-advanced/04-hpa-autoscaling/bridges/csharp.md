---
from: csharp
status: draft
concept: "Automatic elasticity"
from_way: "Autoscale rules on an App Service plan or a VM scale set, and Azure Functions scaling on queue triggers."
native_way: "The HPA plays the App Service autoscale role, and KEDA (built into AKS as an add-on) plays the Functions queue-trigger role for any Deployment."
sensei_tip: "KEDA was started by Microsoft and Red Hat; on AKS enable the KEDA add-on and scale workers on Service Bus queue length."
---

| Azure / .NET world | Kubernetes |
| --- | --- |
| App Service autoscale rules | an HPA |
| a Service Bus triggered Function | a KEDA ScaledObject or ScaledJob |
| a VM scale set | a node pool with the Cluster Autoscaler |
| scale-in cool-down | `behavior.scaleDown.stabilizationWindowSeconds` |
