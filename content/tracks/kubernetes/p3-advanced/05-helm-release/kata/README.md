# A Production Release with Helm

`chart/` is a Helm chart for the `ledger` service. The dojo renders it twice, as the release
`ledger`:

```text
default:  helm template ledger chart
prod:     helm template ledger chart -f chart/values-prod.yaml
```

| Rule | Where |
| --- | --- |
| the Deployment rolls out with `maxSurge` and `maxUnavailable` from `.Values.strategy` (25% and 0) | `templates/deployment.yaml` |
| the Pod template has an annotation `checksum/config` with the SHA-256 of the rendered ConfigMap, so a config change restarts the Pods | `templates/deployment.yaml` |
| the Deployment sets `replicas` **only** when autoscaling is off | `templates/deployment.yaml` |
| an `autoscaling/v2` HPA named like the Deployment, on CPU utilisation, with min, max, target and the scale-down stabilization window from `.Values.autoscaling`, **only** when `autoscaling.enabled` | `templates/hpa.yaml` |
| a PodDisruptionBudget with `minAvailable` from `.Values.podDisruptionBudget`, selecting the chart's Pods, **only** when `podDisruptionBudget.enabled` | `templates/pdb.yaml` |
| prod: image tag `1.8.0`, `LOG_LEVEL: warn`, requests `cpu: 250m`, `memory: 256Mi`, memory limit `512Mi` | `values-prod.yaml` |
| prod: autoscaling on, 3 to 10 replicas at 70 % CPU; a disruption budget with `minAvailable: 2` | `values-prod.yaml` |

With the default values nothing changes: 1 replica, no HPA, no PDB.

```powershell
helm template ledger chart -f chart/values-prod.yaml
helm upgrade --install ledger chart -f chart/values-prod.yaml -n ledger --create-namespace
helm history ledger -n ledger
```

There are 13 tests: 1 in `tests/visible.bats`, 10 hidden ones and the kubeconform validation
of each rendering.
