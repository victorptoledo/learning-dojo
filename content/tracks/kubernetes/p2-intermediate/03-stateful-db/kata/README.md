# A Database with a StatefulSet

`postgres.yaml` holds four objects. The Secret is done; complete the others.

| Rule | Object |
| --- | --- |
| a **headless** Service `postgres` on port `5432` named `postgres`, selecting `app.kubernetes.io/name: postgres` | Service |
| a StatefulSet `postgres` with **3** replicas that uses that Service as its `serviceName` | StatefulSet |
| one container `postgres`, image `postgres:18.1`, port `5432` named `postgres` | StatefulSet |
| `POSTGRES_PASSWORD` from the Secret `postgres-auth`, key `password` (not a literal value) | StatefulSet |
| `PGDATA` set to `/var/lib/postgresql/data/pgdata` | StatefulSet |
| a volume claim template `data`: `ReadWriteOnce`, class `fast-ssd`, `20Gi`, mounted at `/var/lib/postgresql/data` | StatefulSet |
| readiness and liveness probes (an `exec` of `pg_isready -U postgres` is a good choice) | StatefulSet |
| requests `cpu: 500m`, `memory: 1Gi`; a memory limit of `1Gi` | StatefulSet |
| replicas spread over nodes: a topology spread constraint on `kubernetes.io/hostname` with `maxSkew: 1` | StatefulSet |
| a PodDisruptionBudget `postgres` that allows at most **1** unavailable member | PodDisruptionBudget |

On a cluster with a `fast-ssd` StorageClass (or change it to your default class):

```powershell
kubectl apply -f postgres.yaml
kubectl get sts,pods,pvc -l app.kubernetes.io/name=postgres
kubectl run -it --rm psql --image=postgres:18.1 -- psql -h postgres-0.postgres -U postgres
```

There are 12 tests: 1 in `tests/visible.bats`, 10 hidden ones and the kubeconform validation.
