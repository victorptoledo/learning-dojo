# Security Contexts, Pod Security Standards and NetworkPolicy

By default a container in Kubernetes may run as root, write to its file system, keep the
Linux capabilities the runtime grants, and talk to every other Pod in the cluster. When one
container is compromised, all of that is the attacker's. This lesson closes those doors,
following the NSA/CISA Kubernetes Hardening Guide and the Pod Security Standards.

## Security contexts

`securityContext` exists at Pod level (defaults for all containers) and container level
(overrides):

```yaml
spec:
  automountServiceAccountToken: false
  securityContext:
    runAsNonRoot: true                 # refuse to start if the image would run as UID 0
    runAsUser: 10001
    runAsGroup: 10001
    fsGroup: 10001                     # group ownership for mounted volumes
    seccompProfile: { type: RuntimeDefault }   # block rarely needed syscalls
  containers:
    - name: api
      image: ghcr.io/acme/api:3.2.0
      securityContext:
        allowPrivilegeEscalation: false  # no setuid gains (no_new_privs)
        readOnlyRootFilesystem: true     # writes only to volumes
        capabilities: { drop: [ALL] }    # add back only what is needed (NET_BIND_SERVICE)
      volumeMounts: [{ name: tmp, mountPath: /tmp }]
  volumes:
    - { name: tmp, emptyDir: {} }
```

Each line removes an attacker tool: root and setuid binaries, kernel capabilities, dangerous
syscalls, and the ability to drop scripts into the image's directories. Avoid
`privileged: true`, `hostNetwork`, `hostPID`, `hostIPC` and `hostPath` volumes; they hand the
node to the container. The image must cooperate: a numeric non-root `USER` (Docker track,
Phase 2) and no writes outside its volumes.

## Pod Security Standards and admission

The **Pod Security Standards** define three profiles:

| Profile | For | Blocks |
| --- | --- | --- |
| `privileged` | system components (CNI, storage drivers) | nothing |
| `baseline` | most workloads, minimal friction | known privilege escalations: privileged, host namespaces, hostPath, dangerous capabilities |
| `restricted` | hardened workloads | baseline plus: must run as non-root, no privilege escalation, drop ALL capabilities, seccomp RuntimeDefault, limited volume types |

The built-in **Pod Security Admission** controller enforces them per namespace with labels:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: shop
  labels:
    pod-security.kubernetes.io/enforce: restricted       # reject violating Pods
    pod-security.kubernetes.io/enforce-version: v1.36
    pod-security.kubernetes.io/warn: restricted          # warn kubectl users
    pod-security.kubernetes.io/audit: restricted         # record in the audit log
```

Roll out with `warn` and `audit` first, fix what they report, then `enforce`. Admission
checks Pods, so a Deployment with a bad template is accepted but its ReplicaSet fails to
create Pods: look at the ReplicaSet's events. For rules beyond the standards (allowed
registries, required labels, no `latest`), use a policy engine such as Kyverno or OPA
Gatekeeper, or the built-in ValidatingAdmissionPolicy with CEL expressions.

## NetworkPolicy

By default every Pod can reach every other Pod. A **NetworkPolicy** selects Pods and lists
the traffic allowed to (ingress) or from (egress) them. Once any policy selects a Pod for a
direction, everything not allowed in that direction is **denied**. Policies only add allows,
and they need a CNI plugin that enforces them (Calico, Cilium and most managed offerings do;
plain Flannel does not).

Start with a default deny for the namespace:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata: { name: default-deny, namespace: shop }
spec:
  podSelector: {}                      # every Pod in the namespace
  policyTypes: [Ingress, Egress]       # no rules: nothing allowed
```

Then allow what is needed:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata: { name: api, namespace: shop }
spec:
  podSelector: { matchLabels: { app: api } }
  policyTypes: [Ingress, Egress]
  ingress:
    - from:
        - namespaceSelector: { matchLabels: { kubernetes.io/metadata.name: infra } }
          podSelector: { matchLabels: { app: gateway } }   # same item: AND
      ports: [{ port: 8080, protocol: TCP }]
  egress:
    - to:
        - podSelector: { matchLabels: { app: postgres } }
      ports: [{ port: 5432, protocol: TCP }]
    - to:
        - namespaceSelector: { matchLabels: { kubernetes.io/metadata.name: kube-system } }
          podSelector: { matchLabels: { k8s-app: kube-dns } }
      ports: [{ port: 53, protocol: UDP }, { port: 53, protocol: TCP }]
```

Details that bite:

- a `namespaceSelector` and a `podSelector` in the **same** `from` item mean AND; as two
  items they mean OR, which is often far wider than intended;
- with egress denied, **DNS** must be allowed explicitly, or every name lookup fails;
- ports are the **container** ports, not Service ports, because policies see Pod traffic;
- `ipBlock` handles traffic outside the cluster; block the cloud metadata address
  (169.254.169.254) for workloads that do not need it.

## Summary

- Run as non-root, without privilege escalation, with ALL capabilities dropped, seccomp
  RuntimeDefault and a read-only root file system.
- Pod Security Admission enforces privileged, baseline or restricted per namespace by label.
- NetworkPolicies are allow-lists; a Pod selected by any policy is denied everything else.
- Start from default deny for ingress and egress, then allow callers, dependencies and DNS.
- A policy engine adds organisation-specific rules beyond the standards.

## Further reading

- Pod Security Standards: <https://kubernetes.io/docs/concepts/security/pod-security-standards/>
- Pod Security Admission: <https://kubernetes.io/docs/concepts/security/pod-security-admission/>
- Configure a security context: <https://kubernetes.io/docs/tasks/configure-pod-container/security-context/>
- Network policies: <https://kubernetes.io/docs/concepts/services-networking/network-policies/>
- NSA/CISA Kubernetes Hardening Guide: <https://www.cisa.gov/news-events/alerts/2022/03/15/updated-kubernetes-hardening-guide>
