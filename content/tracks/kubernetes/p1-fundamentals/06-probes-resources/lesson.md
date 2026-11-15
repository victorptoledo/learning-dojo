# Probes, Requests and Limits, and QoS Classes

Kubernetes knows whether a process is running, but not whether it works. **Probes** tell it
when a container has started, when it can take traffic and when it is stuck. **Requests and
limits** tell it how much CPU and memory a container needs, so it can place Pods and keep a
greedy one from starving its neighbours.

## Three probes

| Probe | Question | On failure |
| --- | --- | --- |
| `startupProbe` | has it finished starting? | keep waiting; after `failureThreshold` the container is restarted. Other probes wait until it succeeds |
| `readinessProbe` | can it take traffic now? | the Pod is removed from Service endpoints; nothing is restarted |
| `livenessProbe` | is it stuck beyond repair? | the kubelet **restarts** the container |

```yaml
containers:
  - name: orders
    image: ghcr.io/acme/orders:1.4.0
    ports: [{ name: http, containerPort: 8080 }]
    startupProbe:
      httpGet: { path: /health/live, port: http }
      periodSeconds: 5
      failureThreshold: 24          # up to 2 minutes to start
    readinessProbe:
      httpGet: { path: /health/ready, port: http }
      periodSeconds: 5
      failureThreshold: 3
    livenessProbe:
      httpGet: { path: /health/live, port: http }
      periodSeconds: 10
      failureThreshold: 3
      timeoutSeconds: 2
```

Mechanisms: `httpGet` (2xx or 3xx is success), `tcpSocket`, `grpc` and `exec` (a command that
exits 0). Refer to ports by **name**, so they cannot drift from the container port.

Rules that prevent outages:

- **Liveness checks the process only.** If it checks the database, a database blip restarts
  every replica at once and turns a small incident into a big one. Dependencies belong in
  readiness, if anywhere.
- **Slow starters get a startup probe**, instead of a long `initialDelaySeconds` that also
  delays detection later.
- **Readiness gates rollouts.** Without it a started container counts as ready.
- Not every service needs a liveness probe; a process that crashes on error is restarted
  anyway. Add one for deadlocks and hangs.

## Requests and limits

```yaml
    resources:
      requests: { cpu: 250m, memory: 512Mi }
      limits:   { memory: 768Mi }
```

CPU is counted in cores (`250m` is a quarter core), memory in bytes (`Mi`, `Gi`).

- **Requests** are what the scheduler reserves: a Pod goes only on a node whose free
  allocatable capacity covers its requests. They also weigh CPU sharing under contention.
- **Limits** are enforced by the kernel through cgroups:
  - over the **memory** limit, the container is **OOM killed**: exit code **137**, reason
    `OOMKilled`, then restarted;
  - over the **CPU** limit, the container is **throttled**: slower, never killed.

Common practice: always set requests; set a memory limit (equal to or a bit above the
request); be careful with CPU limits, since throttling causes latency spikes even when the
node is idle. Runtimes must know their limits: the JVM sizes its heap from the cgroup limit,
and Go reads `GOMAXPROCS` and `GOMEMLIMIT` from settings you should align with them.

## QoS classes

Kubernetes derives a **QoS class** for each Pod, which decides who is evicted first when a
node runs out of memory:

| Class | When | Evicted |
| --- | --- | --- |
| `Guaranteed` | every container has CPU and memory requests **equal** to limits | last |
| `Burstable` | at least one request or limit, but not Guaranteed | in between |
| `BestEffort` | no requests or limits at all | first |

## Defaults and budgets per namespace

A **LimitRange** sets default requests and limits (and minimums and maximums) for containers
that do not declare them. A **ResourceQuota** caps the total a namespace may request (CPU,
memory, number of Pods, Services, PVCs…). With a quota on CPU or memory, Pods without
requests are rejected, which is a good reason to always set them.

`kubectl top pods` (with the metrics server) shows real usage: set requests from what you
measure, not from guesses.

## Summary

- Startup waits for slow starts, readiness controls traffic, liveness restarts hung containers.
- Liveness must not depend on other services; readiness gates rollouts.
- Requests drive scheduling; memory limits kill (137, OOMKilled), CPU limits throttle.
- QoS classes (Guaranteed, Burstable, BestEffort) follow from requests and limits.
- LimitRange sets per-container defaults; ResourceQuota caps a namespace.

## Further reading

- Liveness, readiness and startup probes: <https://kubernetes.io/docs/concepts/configuration/liveness-readiness-startup-probes/>
- Configure probes (task): <https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/>
- Resource management for Pods and containers: <https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/>
- Pod Quality of Service classes: <https://kubernetes.io/docs/concepts/workloads/pods/pod-qos/>
- Resource quotas: <https://kubernetes.io/docs/concepts/policy/resource-quotas/>
