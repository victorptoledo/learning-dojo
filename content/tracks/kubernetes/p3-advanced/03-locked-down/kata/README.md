# Lock Down a Namespace

The `inventory` service runs in namespace `inventory`. It serves HTTP on 8080 to the
`gateway` Pods of namespace `infra`, uses PostgreSQL (`app.kubernetes.io/name: postgres`, port
5432) in its own namespace, and reads one ConfigMap, `inventory-flags`, through the
Kubernetes API. Harden the four files.

| File | Rule |
| --- | --- |
| `namespace.yaml` | Pod Security Admission **enforces** `restricted` (and warns on it) |
| `rbac.yaml` | the ServiceAccount `inventory` is the only identity of the Pods; its Role allows only `get` and `watch` on the ConfigMap named `inventory-flags`: no wildcards, no Secrets, no `cluster-admin` |
| `deployment.yaml` | the Pods use the ServiceAccount `inventory` and meet the **restricted** profile: `runAsNonRoot` with UID `10001`, `allowPrivilegeEscalation: false`, all capabilities dropped, seccomp `RuntimeDefault` |
| `deployment.yaml` | the root file system is read-only, with an `emptyDir` mounted at `/tmp` |
| `netpol.yaml` | a default deny for ingress **and** egress for every Pod in the namespace |
| `netpol.yaml` | the inventory Pods accept TCP 8080 only from the `gateway` Pods (`app.kubernetes.io/name: gateway`) of namespace `infra`, and may only reach `postgres` on TCP 5432 and the cluster DNS (`k8s-app: kube-dns` in `kube-system`, port 53 UDP and TCP) |

The checker applies the same rules as Pod Security Admission's restricted profile and an
RBAC least-privilege policy over the rendered objects.

```powershell
kubectl apply -f namespace.yaml -f rbac.yaml -f deployment.yaml -f netpol.yaml
kubectl auth can-i list secrets -n inventory --as=system:serviceaccount:inventory:inventory   # no
kubectl auth can-i get configmap/inventory-flags -n inventory --as=system:serviceaccount:inventory:inventory   # yes
```

There are 12 tests: 1 in `tests/visible.bats`, 10 hidden ones and the kubeconform validation.
