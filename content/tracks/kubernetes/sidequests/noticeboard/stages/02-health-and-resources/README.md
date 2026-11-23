Kubernetes only knows a container is running. Tell it when each part is ready for traffic and
when it is stuck, and how much CPU and memory it needs.

**The API** runs its database migrations when it starts, which can take up to a minute, and
then serves `GET /ready` (it can reach its storage) and `GET /health` (the process is alive).

- A `startupProbe` `GET /health` on the port `http` that allows **at least 60 seconds**
  (`periodSeconds` × `failureThreshold`) before giving up.
- A `readinessProbe` `GET /ready` and a `livenessProbe` `GET /health`, both on `http`. The
  liveness probe has no `initialDelaySeconds`: the startup probe already holds it back.
- **Guaranteed** QoS: requests equal limits, `cpu: 250m` and `memory: 256Mi`. The API is the
  last thing the node should evict.

**The web part** starts in a second and serves `GET /` and `GET /healthz`.

- A `readinessProbe` `GET /` and a `livenessProbe` `GET /healthz`, both on `http`; no
  startup probe.
- **Burstable** QoS: requests `cpu: 50m` and `memory: 64Mi`, a memory limit of `128Mi` and
  **no CPU limit** (a CPU limit throttles a page that renders in bursts; memory cannot be
  throttled, only reclaimed by the OOM killer).

Every probe aims at a port its container declares, by name.

> [!TIP]
> A liveness probe that checks dependencies (the database, another service) restarts
> healthy Pods when the dependency is down, all of them at once. Keep it about the process
> itself; readiness is the probe that takes a Pod out of the Service.
