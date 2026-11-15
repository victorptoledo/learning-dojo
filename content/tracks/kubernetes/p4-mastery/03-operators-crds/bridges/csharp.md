---
from: csharp
status: draft
concept: "Your own declarative APIs"
from_way: "An internal portal and a background service that provision resources through Azure Resource Manager."
native_way: "A CRD plus a controller give the same self-service through kubectl and GitOps, reconciled continuously."
sensei_tip: "KubeOps generates the CRD from a C# entity class and wires an IEntityController<T> with reconcile and finalizer methods into the generic host."
---

| .NET world | Kubernetes |
| --- | --- |
| an ARM template resource type | a CRD |
| a `BackgroundService` that provisions | a controller |
| `IValidatableObject` | CEL validation rules |
| Azure Service Operator | an operator for cloud resources |
