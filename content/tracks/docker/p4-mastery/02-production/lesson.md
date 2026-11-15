# Docker in Production: Signals, Orchestration, Updates and the Runtime Underneath

In production, containers start and stop all the time: deployments, scaling, node failures,
rebalancing. A service that runs fine on `docker run` but drops requests whenever it is
replaced is not production-ready. This lesson collects what makes the difference.

## Services built for containers

The **twelve-factor** principles still describe a container-friendly service well:

- **Configuration from the environment** (and secrets from files): one image for every
  environment, never rebuilt per stage.
- **Stateless processes**: state lives in databases, caches, object storage or volumes, so
  any replica can be killed and replaced.
- **Disposability**: start fast, and **shut down gracefully** on `SIGTERM`: stop accepting
  new work, finish in-flight requests within the grace period, close connections, exit.
- **Logs as streams** to stdout (previous lesson).
- **Port binding**: the service is self-contained and listens on a port.

Add to that **health endpoints**: *liveness* ("restart me if this fails") and *readiness*
("send me traffic only when this passes", which stays false while starting up or
draining). Keep liveness cheap and local; put dependency checks in readiness.

## PID 1, again

Your process is PID 1 and must handle signals and reap children (Phase 1). In production:

- Use the exec form; add `--init` (or `tini`) when the program spawns children.
- Choose `STOPSIGNAL` if the program expects another signal (nginx uses `SIGQUIT` for a
  graceful stop).
- Align timeouts: `docker stop -t`, Compose `stop_grace_period`, Kubernetes
  `terminationGracePeriodSeconds` must be longer than the app's own shutdown timeout.

## A pipeline for images

```text
commit → build (BuildKit, cache from registry) → unit tests (--target test)
       → scan (fail on fixable HIGH/CRITICAL) → SBOM + provenance → sign
       → push 1.4.0 and sha-3f9c2a1 → deploy by digest → smoke test → promote
```

Build **once** and promote the same digest through staging and production. Rebuilding per
environment means production runs something that was never tested.

## Updating without downtime

On one host, Compose replaces containers in place, so there is a gap. Zero-downtime updates
need more than one replica and something in front of them:

- **Rolling update**: start a new replica, wait until it is *ready*, move traffic, stop an
  old one (which drains on `SIGTERM`), repeat.
- **Blue-green**: run the full new version next to the old one and switch the router at
  once; switch back to roll back.
- **Canary**: send a small share of traffic to the new version first and watch errors and
  latency.

Rollback is the same operation with the previous digest. Database migrations must be
**backward compatible** (expand, migrate, contract), because old and new versions run at the
same time during an update.

## Orchestrators

When you need several hosts, self-healing, rolling updates, service discovery and
autoscaling, an **orchestrator** schedules containers for you:

| Option | Fits |
| --- | --- |
| Docker Compose on one host | development, CI, small internal tools |
| Docker Swarm | small clusters, Compose-like files, little to learn |
| Kubernetes (managed: EKS, GKE, AKS) | the industry default for many services and teams |
| ECS, Cloud Run, Azure Container Apps | managed platforms that run your image without a cluster to operate |
| Nomad | mixed workloads, simpler operations |

Everything in this track carries over: the same images, health checks, limits, non-root
users, read-only file systems, secrets as files, logs to stdout. Kubernetes uses its own
probes instead of `HEALTHCHECK`, and its `securityContext` mirrors the runtime flags of
Phase 3.

## Underneath the engine

```text
docker CLI ──API──▶ dockerd ──▶ containerd ──▶ containerd-shim ──▶ runc ──▶ your process
                     (images,     (images,        (one per          (creates namespaces,
                     networks,     snapshots,      container,        cgroups, mounts per
                     volumes,      lifecycle)      keeps it alive)   the OCI runtime spec,
                     builds)                                         then exits)
```

- **runc** is the reference implementation of the **OCI runtime specification**: given a
  bundle (a root file system and a `config.json`), it sets up the namespaces, cgroups,
  capabilities, seccomp filter and mounts described in Phase 1 and 3, starts the process
  and exits.
- **containerd** manages images, snapshots (the layers) and container lifecycles; the
  **shim** keeps each container running independently of the daemon, which is why
  `live-restore` can upgrade `dockerd` without stopping containers.
- Kubernetes talks to containerd (or CRI-O) through the Container Runtime Interface; it does
  not need Docker at all, but it runs the same OCI images.
- Other OCI runtimes plug in at the runc level: `crun` (faster), gVisor's `runsc`, Kata
  Containers.

The Linux track's lesson on the kernel, cgroups and namespaces (`linux.p4`) shows these
pieces by hand.

## A production checklist

| Area | Ready when |
| --- | --- |
| Image | multi-stage, minimal base, non-root numeric user, no secrets, labelled, scanned, signed |
| Runtime | limits set, capabilities dropped, read-only root, no privileged flags or socket |
| Lifecycle | exec-form PID 1, graceful `SIGTERM`, timeouts aligned, readiness and liveness |
| Delivery | build once, deploy by digest, rolling or blue-green updates, tested rollback |
| Operations | logs centralised and rotated, metrics and alerts, backups of volumes tested |

## Summary

- Twelve-factor services: config from the environment, stateless, disposable, logs to
  stdout, health endpoints.
- PID 1 must stop gracefully within aligned timeouts.
- Build once, promote the same digest; scan, sign and deploy by digest.
- Zero downtime needs replicas, readiness and draining; migrations must be backward
  compatible.
- Orchestrators take over scheduling and updates across hosts; the image practices stay the
  same.
- The engine sits on containerd, a shim and runc, implementing the OCI specifications.

## Further reading

- The Twelve-Factor App: <https://12factor.net/>
- Kubernetes: containers and images: <https://kubernetes.io/docs/concepts/containers/>
- Using Compose in production: <https://docs.docker.com/compose/how-tos/production/>
- Live restore: <https://docs.docker.com/engine/daemon/live-restore/>
- containerd: <https://containerd.io/>
- runc and the OCI runtime specification: <https://github.com/opencontainers/runc>, <https://github.com/opencontainers/runtime-spec>
- GitHub Actions with Docker Build: <https://docs.docker.com/build/ci/github-actions/>
