# Authentication, RBAC and ServiceAccounts

Every change to a cluster is an API request, and every API request answers three questions
before it is stored: **who** are you, **may** you do this, and does the request **pass** the
cluster's policies? Controlling the second question well is most of Kubernetes security.

## The request path

1. **Authentication**: who is calling. Kubernetes has no user database. People are
   identified by client certificates, OIDC tokens from an identity provider (Entra ID,
   Google, Okta, Dex) or a cloud plugin (`aws eks get-token`); workloads by **ServiceAccount
   tokens**. The result is a user name and groups, for example
   `system:serviceaccount:shop:orders` in group `system:serviceaccounts:shop`.
2. **Authorisation**: may this identity perform this **verb** on this **resource** in this
   **namespace**? Usually decided by **RBAC**; nodes use the Node authoriser.
3. **Admission**: mutating and validating admission controllers set defaults and enforce
   policies (Pod Security, quotas, webhooks such as Kyverno or Gatekeeper).

## RBAC objects

| Object | Scope | Says |
| --- | --- | --- |
| **Role** | a namespace | a list of rules: apiGroups, resources, verbs (and optional resourceNames) |
| **ClusterRole** | the cluster | the same, for cluster-wide resources or to reuse in many namespaces |
| **RoleBinding** | a namespace | gives a Role **or** a ClusterRole to subjects, **in that namespace only** |
| **ClusterRoleBinding** | the cluster | gives a ClusterRole to subjects in **every** namespace |

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata: { name: config-reader, namespace: shop }
rules:
  - apiGroups: [""]                 # "" is the core group (pods, configmaps, secrets…)
    resources: [configmaps]
    verbs: [get, list, watch]
  - apiGroups: [""]
    resources: [configmaps]
    resourceNames: [feature-flags]  # only this object
    verbs: [update]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata: { name: orders-config-reader, namespace: shop }
subjects:
  - { kind: ServiceAccount, name: orders, namespace: shop }
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: config-reader
```

RBAC is **allow-only**: there are no deny rules, and permissions add up across bindings.
Verbs are `get`, `list`, `watch`, `create`, `update`, `patch`, `delete`, `deletecollection`,
plus special ones; subresources are written `pods/log`, `pods/exec`, `deployments/scale`.
Built-in ClusterRoles `view`, `edit`, `admin` and `cluster-admin` cover common needs: bind
`view` or `edit` with a **RoleBinding** to limit them to one namespace.

## Testing permissions

```powershell
kubectl auth can-i list secrets -n shop
kubectl auth can-i patch deployments -n shop --as=system:serviceaccount:ci:deployer
kubectl auth can-i --list --as=system:serviceaccount:shop:orders -n shop
kubectl auth whoami
```

Test both what should work and what should not; put these checks in CI for important roles.

## ServiceAccounts

Every Pod runs as a **ServiceAccount**, the `default` one of its namespace unless you name
another. The kubelet mounts a short-lived, audience-bound token (rotated automatically, via a
projected volume) at `/var/run/secrets/kubernetes.io/serviceaccount/`, which client libraries
use to call the API.

Practices:

- one ServiceAccount **per workload**, with only the Roles it needs;
- most applications never call the Kubernetes API: set `automountServiceAccountToken: false`
  on the ServiceAccount or the Pod, so a compromised container has no token to steal;
- never grant rights to the `default` ServiceAccount;
- the same tokens let workloads authenticate to clouds (IRSA or EKS Pod Identity, GKE and
  Azure Workload Identity) without static keys.

## Least privilege and escalation

Some permissions are worth more than they look:

| Permission | Why it is powerful |
| --- | --- |
| `create` on pods (or Deployments, Jobs…) | the Pod can mount any Secret and use any ServiceAccount of the namespace |
| `get`/`list` on secrets | reads every token and password in scope |
| `escalate`, `bind` | grant yourself permissions you do not have |
| `impersonate` | act as another user or group |
| `pods/exec`, `nodes/proxy` | run commands in containers or talk to kubelets |
| wildcards `*` | everything, including resources added in future versions |

The RBAC API itself prevents escalation by default: you cannot create a Role granting more
than you hold, unless you have `escalate`. Audit regularly (`kubectl auth can-i --list`,
tools such as rbac-tool or Kubescape), and prefer groups from your identity provider over
binding individual users.

## Summary

- Requests pass authentication, authorisation (RBAC) and admission, in that order.
- Roles and ClusterRoles list allowed verbs on resources; bindings give them to subjects.
- A RoleBinding limits even a ClusterRole to one namespace; RBAC has no deny rules.
- Each workload gets its own ServiceAccount; disable token mounting when it is not needed.
- Watch for escalation paths: creating Pods, reading Secrets, escalate, bind, impersonate,
  wildcards.

## Further reading

- Controlling access to the API: <https://kubernetes.io/docs/concepts/security/controlling-access/>
- Using RBAC authorisation: <https://kubernetes.io/docs/reference/access-authn-authz/rbac/>
- RBAC good practices: <https://kubernetes.io/docs/concepts/security/rbac-good-practices/>
- Service accounts: <https://kubernetes.io/docs/concepts/security/service-accounts/>
- Kubernetes security roadmap (roadmap.sh): <https://roadmap.sh/kubernetes>
