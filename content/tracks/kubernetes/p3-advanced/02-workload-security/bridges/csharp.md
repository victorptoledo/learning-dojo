---
from: csharp
status: draft
concept: "Least privilege for the process and the network"
from_way: "IIS application pool identities and Windows Firewall rules per server."
native_way: "A non-root security context replaces the pool identity, and NetworkPolicies replace per-server firewall rules with rules per workload."
sensei_tip: "The .NET 8+ images include an app user (UID 1654) and listen on 8080 by default, so they meet the restricted standard with runAsNonRoot and no added capabilities."
---

| .NET world | Kubernetes |
| --- | --- |
| an application pool identity | `runAsUser` |
| Windows Firewall rules | NetworkPolicies |
| a group policy baseline | Pod Security Standards |
| Defender application control | admission policies (Kyverno, Gatekeeper) |
