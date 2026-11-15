---
from: csharp
status: draft
concept: "An edge proxy configured by objects"
from_way: "IIS bindings and URL Rewrite rules, or YARP routes in a gateway project, or Azure Application Gateway."
native_way: "Routing rules become Ingress or Gateway API objects, implemented by a controller (including Application Gateway for Containers on AKS)."
sensei_tip: "Add UseForwardedHeaders with KnownNetworks cleared (or set to the cluster range) so ASP.NET Core trusts X-Forwarded-* from the in-cluster proxy."
---

| .NET world | Kubernetes |
| --- | --- |
| IIS site bindings | Gateway listeners / Ingress hosts |
| URL Rewrite rules | HTTPRoute filters |
| YARP route config | HTTPRoute rules |
| a certificate in the Windows store | a TLS Secret |
