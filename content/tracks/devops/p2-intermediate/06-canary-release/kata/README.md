# A Canary Release with the Gateway API

`checkout` runs version 1.8.0 (`stable.yaml`) behind an HTTPRoute (`route.yaml`). Version
1.9.0 is ready. Instead of rolling it out to everyone, release it as a **canary**: one
replica that receives 10% of the traffic, plus a header that lets testers reach it on
purpose. If its error rate and latency hold, you raise the weight step by step; if not, you
set it to 0 and nobody else is affected.

| File | Rule |
| --- | --- |
| `canary.yaml` | a Deployment `checkout-canary`: **1** replica, image `ghcr.io/acme/checkout:1.9.0`, Pods labelled `app: checkout` and `track: canary`, the same container name, port, probes and resources as stable |
| `canary.yaml` | a Service `checkout-canary` (port 80 to the container port `http`) that selects only the canary's Pods |
| `stable.yaml` | fix the Service `checkout-stable`: today it selects `app: checkout` only, so it would also send traffic to the canary's Pods. Each Service must select exactly one track |
| `route.yaml` | keep the parent Gateway (`public` in `infra`) and the host `shop.example.com` |
| `route.yaml` | rule 1: requests with the header `x-canary: always` (an `Exact` match) go to `checkout-canary` only |
| `route.yaml` | rule 2: path prefix `/` is split by weight, **90** to `checkout-stable` and **10** to `checkout-canary` |

The dojo validates the manifests against the Kubernetes 1.36 and Gateway API schemas and
reads them with bats; no cluster is needed. On a cluster with a Gateway API implementation:

```powershell
kubectl apply -f stable.yaml -f canary.yaml -f route.yaml
curl -H "x-canary: always" https://shop.example.com/version   # always 1.9.0
```

There are 10 tests: 1 in `tests/visible.bats`, 8 hidden ones and the kubeconform validation.
