# Debugging Workloads: describe, Events, Logs and Ephemeral Containers

Most Kubernetes incidents are not exotic. A label does not match, a port is wrong, a probe
fails, a limit is too low, a Secret key is missing. What turns a two-hour outage into a
ten-minute one is a **method**: start from the symptom, follow the chain of objects, and let
the cluster tell you what it already knows.

## Follow the chain

```text
client → DNS → Gateway / Ingress → Service → EndpointSlice → Pod (ready?) → container
                                                    ↑
                              Deployment → ReplicaSet → Pods (scheduled? started? crashing?)
```

```powershell
kubectl get deploy,rs,pods -n shop -o wide         # what exists, how many ready, restarts
kubectl describe deploy checkout -n shop           # conditions: Progressing, Available
kubectl describe pod checkout-6c9f8-x2k4p -n shop  # container states, reasons, exit codes, events
kubectl get events -n shop --sort-by=.lastTimestamp
kubectl get endpointslices -n shop -l kubernetes.io/service-name=checkout
kubectl rollout history deploy/checkout -n shop    # what changed recently?
```

`describe` is the most useful command in Kubernetes: it joins the object, its status and
the **events** about it. Events expire after about an hour, so look early.

## Reading Pod states

| You see | It means | Look at |
| --- | --- | --- |
| `Pending` | not scheduled yet | events: `Insufficient cpu`, untolerated taint, unbound PVC, quota |
| `ContainerCreating` for long | volume or network setup failing | events: mount errors, CNI errors, missing Secret or ConfigMap |
| `ImagePullBackOff` / `ErrImagePull` | the image cannot be pulled | the tag exists? registry credentials (`imagePullSecrets`)? |
| `CreateContainerConfigError` | a referenced ConfigMap, Secret or key does not exist | describe: the exact missing name |
| `CrashLoopBackOff` | the process keeps exiting | `kubectl logs --previous`, the exit code |
| `Running` but `0/1 READY` | the readiness probe fails | describe: probe failures; the probe's port and path |
| `OOMKilled`, exit code 137 | over the memory limit | the limit, real usage (`kubectl top`), heap settings |
| exit code 1, 2… | the application failed | the logs |
| `Evicted` | node pressure (memory, disk) | node conditions, QoS class |

## Logs

```powershell
kubectl logs deploy/checkout -n shop                # one Pod of the Deployment
kubectl logs checkout-6c9f8-x2k4p -c app --previous # the run before the last restart
kubectl logs -l app=checkout -n shop --prefix --since=15m
kubectl logs -f checkout-6c9f8-x2k4p                 # follow
```

`--previous` is the key for crash loops: the current container may not have printed
anything yet. Container logs live on the node and disappear with the Pod; production
clusters ship them to a central store (Loki, Elasticsearch, a cloud service).

## Inside the Pod

```powershell
kubectl exec -it checkout-6c9f8-x2k4p -- sh          # only if the image has a shell
kubectl port-forward deploy/checkout 8080:8080       # call it from your machine
kubectl debug -it checkout-6c9f8-x2k4p --image=busybox:1.37 --target=app
kubectl debug node/worker-3 -it --image=busybox:1.37 # a Pod on the node, host file system at /host
kubectl run tmp -it --rm --image=nicolaka/netshoot -- bash   # test DNS and connectivity from the cluster
```

Minimal and distroless images have no shell, which is good for security and bad for `exec`.
**Ephemeral containers** (`kubectl debug --target`) solve that: a debugging container joins
the running Pod's process namespace, sees its processes and network, and disappears with it.
`kubectl debug --copy-to` creates a copy of the Pod with a changed command, to start a
crashing container in a shell.

## Networking checks

From a debug Pod in the same namespace:

```bash
nslookup checkout                         # DNS: does the name resolve?
wget -qO- http://checkout/health          # through the Service
wget -qO- http://10.244.1.17:8080/health  # straight to a Pod IP: is the app itself fine?
```

If the Pod IP works and the Service does not, look at the selector, the `targetPort` and
readiness. If nothing works, look for a NetworkPolicy denying the traffic (or DNS egress).

## Mitigate first

During an incident, restoring service beats understanding it. If the problem started with a
deploy, `kubectl rollout undo` (or reverting in Git) usually ends it in a minute; investigate
the bad ReplicaSet afterwards. Scale up, cordon a bad node (`kubectl cordon`), or move
traffic, then debug calmly and write down what you learned.

## Summary

- Follow the chain from the edge to the container; `describe` and events say a lot.
- Each Pod state points to a cause: scheduling, image, config, crash, readiness, memory.
- `kubectl logs --previous` explains crash loops; logs vanish with the Pod unless shipped.
- `kubectl debug` adds ephemeral containers to shell-less Pods and debugs nodes.
- Compare Pod IP and Service: a working Pod behind a failing Service is a wiring problem.
- Mitigate (roll back) first, investigate second.

## Further reading

- Debug Pods: <https://kubernetes.io/docs/tasks/debug/debug-application/debug-pods/>
- Debug Services: <https://kubernetes.io/docs/tasks/debug/debug-application/debug-service/>
- Debug running Pods (ephemeral containers): <https://kubernetes.io/docs/tasks/debug/debug-application/debug-running-pod/>
- Determine the reason for Pod failure: <https://kubernetes.io/docs/tasks/debug/debug-application/determine-reason-pod-failure/>
