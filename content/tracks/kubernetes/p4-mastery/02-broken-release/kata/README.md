# Fix a Broken Release

The `checkout` service (release **2.4.0**, listening on 8080, metrics on 9090, `/health`
for the process and `/ready` for the process plus its database) was deployed with the four
files in this folder. Here is the incident channel:

| Time | Symptom reported |
| --- | --- |
| 10:02 | `kubectl get pods`: `ImagePullBackOff`, "manifest for ghcr.io/acme/checkout:2.4 not found" |
| 10:09 | after a hot fix of the tag: `CreateContainerConfigError`, "couldn't find key PAYMENT_URL in ConfigMap checkout-config" |
| 10:15 | the Pods start but stay `0/1 READY`; describe: "Readiness probe failed: connection refused" |
| 10:21 | `kubectl describe svc checkout`: `Endpoints: <none>`, even with a Pod that is ready |
| 10:24 | from another Pod: `curl http://checkout/` → connection refused, with the Pod IP on 8080 answering fine |
| 10:31 | the Pods restart every few minutes with `OOMKilled`, exit code 137; `kubectl top` showed 600Mi just before. The capacity review decided: request `512Mi`, limit `768Mi` |
| 10:40 | the database had a 30-second blip and **every** checkout Pod restarted at the same time |
| 10:55 | the platform team cannot drain nodes for the upgrade: "Cannot evict pod as it would violate the pod's disruption budget" |

Find the cause of every symptom in the manifests and fix it. Do not change what works: the
ConfigMap is correct, the replica count stays 3, the Service stays on port 80.

There are 12 tests: 1 in `tests/visible.bats`, 10 hidden ones and the kubeconform validation.
