# Checkpoint 3: Ship a Hardened, Queue-Scaled Worker

`chart/` is the Helm chart of the `thumbnailer` worker. It reads upload events from
RabbitMQ, renders thumbnails in `/tmp`, and starts a Kubernetes **Job** for the very large
uploads, whose log it copies into the error report when the Job fails. Today the chart is a
bare Deployment that runs as root with the default ServiceAccount, a fixed replica count and
no network rules. Finish the templates; `values.yaml` and `values-prod.yaml` are complete.

The dojo renders the chart three times, as the release `thumbnailer` in namespace `media`:

```text
default:  helm template thumbnailer chart --namespace media
prod:     helm template thumbnailer chart --namespace media -f chart/values-prod.yaml
norbac:   helm template thumbnailer chart --namespace media --set rbac.create=false
```

| Rule | Where |
| --- | --- |
| a ServiceAccount named like the release, **always** created, and the Pods run as it | `serviceaccount.yaml`, `deployment.yaml` |
| when `rbac.create`: a **Role** and a **RoleBinding** named like the release that let the ServiceAccount (in the release namespace) `create`, `get`, `list`, `watch` and `delete` Jobs, `get`, `list` and `watch` Pods, and `get` `pods/log`. Nothing else: no wildcards, no Secrets, no `pods/exec` | `rbac.yaml` |
| the Pods meet the **restricted** Pod Security profile as UID `10001`: `runAsNonRoot`, `allowPrivilegeEscalation: false`, all capabilities dropped, seccomp `RuntimeDefault` | `deployment.yaml` |
| a read-only root file system, with an `emptyDir` of at most `1Gi` mounted at `/tmp` | `deployment.yaml` |
| the Deployment sets `replicas` **only** when autoscaling is off | `deployment.yaml` |
| when `autoscaling.enabled`: an `autoscaling/v2` HPA on the **queue depth**, not on CPU: the External metric `rabbitmq_queue_messages_ready` for the queue `queue.name` (a `matchLabels` selector `queue: ...`), with an `AverageValue` target of `messagesPerReplica`; min and max from the values; at most `scaleUp.pods` Pods added per `scaleUp.periodSeconds`; a scale-down stabilization window of `scaleDownStabilizationSeconds` | `hpa.yaml` |
| when `networkPolicy.enabled`: a NetworkPolicy named like the release for the chart's Pods, for ingress **and** egress | `networkpolicy.yaml` |
| ingress: only the Prometheus Pods of namespace `monitoring` (both selectors in **one** peer), on the `metrics` port | `networkpolicy.yaml` |
| egress: RabbitMQ in namespace `messaging` on TCP 5672; the cluster DNS (`k8s-app: kube-dns` in `kube-system`) on 53 UDP and TCP; the API server at `networkPolicy.apiServer.cidr` and `.port`. Nothing else | `networkpolicy.yaml` |

Use the values, not literals: the tests read prod's numbers (2 to 20 replicas, the API
server at `172.20.0.10/32` on `6443`), which differ from the defaults.

```powershell
helm template thumbnailer chart --namespace media -f chart/values-prod.yaml
kubectl auth can-i create jobs -n media --as=system:serviceaccount:media:thumbnailer       # yes
kubectl auth can-i create pods/exec -n media --as=system:serviceaccount:media:thumbnailer  # no
```

There are 20 tests: 1 in `tests/visible.bats`, 16 hidden ones and the kubeconform validation
of each rendering.
