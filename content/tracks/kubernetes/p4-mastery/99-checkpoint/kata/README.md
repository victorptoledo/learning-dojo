# Checkpoint 4: Final Exam: Ship the Database Operator

The platform team has written an operator: a `Database` custom resource
(`platform.acme.io`) and a controller that turns each one into a StatefulSet, a Service and a
credentials Secret. `chart/` packages it, and it is not ready for production:

- the CRD has only `v1alpha1`, accepts **any** `spec` (a typo such as `storageGb` is silently
  dropped), has no status subresource and shows nothing useful in `kubectl get databases`;
- the controller is bound to `cluster-admin` ("it worked on the first try");
- it runs as root from the `latest` tag, two replicas reconcile at the same time, and its
  readiness probe aims at the metrics port;
- namespace editors cannot create a `Database`, because the built-in roles know nothing about
  the new kind.

Fix the CRD in `chart/crds/databases.yaml` and the templates; `values.yaml` and
`values-prod.yaml` are complete. The dojo renders the release `db-operator` in namespace
`platform`:

```text
prod:  helm template db-operator chart --namespace platform -f chart/values-prod.yaml
```

Helm installs `crds/` before the templates and does not template it, so the tests read the
CRD file directly and run sample `Database` objects through its schema, as the API server
would.

**The CRD** (`crds/databases.yaml`)

| Rule |
| --- |
| `v1` is served and is the **storage** version; `v1alpha1` is still served, not stored, `deprecated`, with a `deprecationWarning` that names `platform.acme.io/v1`. Both have the same schema, so no conversion webhook is needed |
| `spec` requires `engineVersion` (the **string** `"17"` or `"18"`), `size` (`small`, `medium` or `large`) and `storageGB` (an integer from 10 to 1000); `backup` is optional, with a string `schedule` and an integer `retentionDays` from 1 to 90. Unknown fields are rejected; only `status` keeps any shape |
| a CEL **transition rule** on `storageGB` (`self >= oldSelf`, with a message) stops a volume from shrinking, in both versions |
| both versions have the **status subresource** |
| `v1` prints the columns `Version` (`.spec.engineVersion`), `Size` (`.spec.size`), `Ready` (`.status.conditions[?(@.type=="Ready")].status`) and `Age` (`.metadata.creationTimestamp`, type `date`), in that order |

**RBAC** (`templates/rbac.yaml`, `templates/aggregated-roles.yaml`)

| Rule |
| --- |
| a ClusterRole `db-operator`, bound by a ClusterRoleBinding `db-operator` to the ServiceAccount in the release namespace, with exactly: `get`, `list`, `watch`, `update`, `patch` on `databases`; `get`, `update`, `patch` on `databases/status`; `update` on `databases/finalizers`; `get`, `list`, `watch`, `create`, `update`, `patch` on StatefulSets, Services and Secrets; `create` and `patch` on `events.k8s.io` events. No `delete` anywhere: the children carry an ownerReference, and users delete Databases |
| leader election uses a Lease in the release namespace: a **Role** and a RoleBinding `db-operator-leader-election` with `get`, `list`, `watch`, `create`, `update`, `patch` on `coordination.k8s.io` leases |
| no binding to `cluster-admin` (or any other built-in role): the ClusterRoleBinding above is the only one |
| aggregated ClusterRoles: `db-operator-edit` (labelled to aggregate into **edit and admin**) with every verb on `databases` (`get`, `list`, `watch`, `create`, `update`, `patch`, `delete`, `deletecollection`), and `db-operator-view` (aggregated into **view**) with `get`, `list`, `watch`. Neither touches the status |

**The controller** (`templates/deployment.yaml`, `templates/pdb.yaml`)

| Rule |
| --- |
| the image is `repository@digest` from the values; rendering without `image.digest` **fails** with a message that contains `image.digest is required` (`required`) |
| `--leader-elect=true` only when `leaderElection.enabled`; with more than one replica and leader election off, rendering **fails** with a message that names `leaderElection` (`fail`) |
| the restricted Pod Security profile and a read-only root file system |
| readiness `/readyz` and liveness `/healthz` on the `health` port (8081); the metrics Service keeps working |
| the replicas spread over nodes (`kubernetes.io/hostname`, `maxSkew: 1`, selecting the controller's Pods), and a PodDisruptionBudget `db-operator` with `minAvailable: 1` |

```powershell
helm template db-operator chart --namespace platform -f chart/values-prod.yaml
helm template db-operator chart --namespace platform     # must fail: no digest
kubectl apply -f chart/crds/databases.yaml; kubectl explain database.spec
kubectl auth can-i create databases -n shop --as=jane --as-group=shop-editors
```

There are 23 tests: 1 in `tests/visible.bats`, 21 hidden ones and the kubeconform validation
of the rendering.
