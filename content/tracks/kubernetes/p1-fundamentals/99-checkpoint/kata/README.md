# Checkpoint 1: Deploy a Configured Web Service

Ship the `greeter` web service (image `ghcr.io/acme/greeter:1.2.0`, listening on 8080, with
`GET /ready` and `GET /health` endpoints) as three manifests that fit together:
`configmap.yaml`, `deployment.yaml` and `service.yaml`. The starter has gaps and one wiring
bug.

| Rule | Where |
| --- | --- |
| a ConfigMap `greeter-config` with `GREETING_NAME: dojo` and `LOG_LEVEL: info` | `configmap.yaml` |
| a Deployment `greeter` with **3** replicas, Pods labelled `app.kubernetes.io/name: greeter` and a selector that matches them | `deployment.yaml` |
| one container `greeter`, image `ghcr.io/acme/greeter:1.2.0`, port `8080` named `http` | `deployment.yaml` |
| every key of `greeter-config` as an environment variable (`envFrom`) | `deployment.yaml` |
| a readiness probe `GET /ready` and a liveness probe `GET /health`, both on the port named `http` | `deployment.yaml` |
| requests `cpu: 100m`, `memory: 128Mi`; a memory limit of `256Mi` | `deployment.yaml` |
| rolling updates that never lose capacity: `maxSurge: 1`, `maxUnavailable: 0` | `deployment.yaml` |
| a ClusterIP Service `greeter` on port `80` that reaches the Pods on the **named** port `http` | `service.yaml` |

On a cluster (the image is fictional, so the Pods will not start, but every object is
accepted):

```powershell
kubectl apply -f .
kubectl get deploy,rs,pods,svc,endpointslices
kubectl describe svc greeter
```

There are 12 tests: 1 in `tests/visible.bats`, 10 hidden ones and the kubeconform validation.
