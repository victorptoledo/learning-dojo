# Runtime Security: Resource Limits, Capabilities, seccomp and Rootless

A hardened image is half the job. The other half is how the container is **run**: how much of
the machine it can consume, which kernel privileges it keeps, and which parts of the
isolation you leave switched on. The dojo runs your katas this way, and this lesson explains
every flag it uses.

## Resource limits

Without limits, a container may use all the host's memory and CPU. With them:

```powershell
docker run -d --memory 256m --memory-swap 256m --cpus 0.5 --pids-limit 200 shop-api:1.4.0
docker stats                              # live use against the limits
```

| Flag | Over the limit | Note |
| --- | --- | --- |
| `--memory 256m` | the OOM killer kills a process; exit 137, `OOMKilled: true` | set `--memory-swap` equal to it to forbid swap |
| `--cpus 0.5` | the container is **throttled**, never killed | `--cpu-shares` only weighs CPU under contention |
| `--pids-limit 200` | `fork` fails | stops fork bombs and thread leaks |
| `--ulimit nofile=1024:2048` | `open` fails with "too many open files" | per-process limits |
| `--device-read-bps`, `--blkio-weight` | I/O is slowed | disk contention |

Make runtimes aware of the limits: the JVM, .NET, Node.js and Go read cgroup limits to size
heaps and thread pools, but older versions and many libraries look at the host. In Compose
the limits live under `deploy.resources.limits`; in Kubernetes, in `resources.limits`.

## Linux capabilities

Root's power is split into about 40 **capabilities**: `CAP_NET_BIND_SERVICE` (ports below
1024), `CAP_CHOWN`, `CAP_NET_RAW` (raw sockets, used by `ping`), `CAP_SYS_ADMIN` (almost
everything), and so on. Docker gives containers a reduced default set; most services need
none of it:

```powershell
docker run --cap-drop ALL --cap-add NET_BIND_SERVICE nginx-custom   # only what is needed
```

Add `--security-opt no-new-privileges` so no process in the container can gain privileges
through setuid binaries or file capabilities, even if one is in the image.

## A read-only root file system

```powershell
docker run --read-only --tmpfs /tmp --mount source=data,target=/data shop-api:1.4.0
```

An attacker who gets code execution cannot drop tools into the image's directories or modify
the application. Writable paths become explicit: a tmpfs for scratch, a volume for data.
Designing images for `--read-only` from the start is much easier than retrofitting it.

## seccomp and AppArmor

- **seccomp** filters the **system calls** a process may make. Docker's default profile
  blocks around 50 rarely needed and dangerous ones (`mount`, `keyctl`, `bpf`, kernel module
  loading ...). Keep it; write a stricter custom profile for sensitive services with
  `--security-opt seccomp=profile.json`.
- **AppArmor** (Debian and Ubuntu hosts) and **SELinux** (Red Hat family) are mandatory
  access control systems. Docker applies a default AppArmor profile (`docker-default`)
  restricting writes to `/proc` and `/sys` and mounts.

`--security-opt seccomp=unconfined` and `apparmor=unconfined` switch these protections off.
Treat them as red flags in a review.

## The flags that undo isolation

| Flag | Effectively grants |
| --- | --- |
| `--privileged` | every capability, all devices, no seccomp or AppArmor: root on the host |
| `-v /var/run/docker.sock:/var/run/docker.sock` | control of the engine, which can start a privileged container: root on the host |
| `--pid host`, `--network host`, `--ipc host` | the host's processes, network stack or shared memory |
| `-v /:/host` or other sensitive host paths | the host's files |
| `--cap-add SYS_ADMIN` | close to `--privileged` |
| `--user 0` with no user namespace | UID 0 on the host if the container escapes |

Tools that "need" the socket (CI agents, monitoring, Traefik) should get a filtered proxy,
rootless Docker, or a different design. The CIS Docker Benchmark lists all of these checks,
and `docker-bench-security` runs them against a host.

## User namespaces and rootless Docker

- **User namespace remapping** (`userns-remap` in the daemon configuration) maps container
  UID 0 to an unprivileged range on the host, so root inside is nobody outside.
- **Rootless Docker** runs the daemon itself as a normal user: an escape lands in an
  unprivileged account. Podman is rootless by default.
- **Sandboxed runtimes** add a kernel boundary for untrusted code: gVisor (a user-space
  kernel) or Kata Containers (lightweight VMs), selected with `--runtime`.

## Putting it together

```powershell
docker run -d --name api `
  --user 10001:10001 --read-only --tmpfs /tmp `
  --cap-drop ALL --security-opt no-new-privileges `
  --memory 256m --memory-swap 256m --cpus 0.5 --pids-limit 200 `
  --network backend -p 127.0.0.1:8080:8080 `
  --restart unless-stopped `
  ghcr.io/acme/shop-api@sha256:4c1e...
```

## Summary

- Limit memory (OOM kill), CPU (throttling), processes and file handles.
- Drop all capabilities and add back only what is needed; set `no-new-privileges`.
- Run with a read-only root file system and explicit writable mounts.
- Keep the default seccomp and AppArmor profiles; never `unconfined` without a reason.
- `--privileged`, the Docker socket and host namespaces give the host away.
- User namespaces, rootless Docker and sandboxed runtimes reduce what an escape gains.

## Further reading

- Resource constraints: <https://docs.docker.com/engine/containers/resource_constraints/>
- Docker Engine security: <https://docs.docker.com/engine/security/>
- seccomp profiles: <https://docs.docker.com/engine/security/seccomp/>
- AppArmor profiles: <https://docs.docker.com/engine/security/apparmor/>
- Rootless mode: <https://docs.docker.com/engine/security/rootless/>
- User namespace remapping: <https://docs.docker.com/engine/security/userns-remap/>
- capabilities(7): <https://man7.org/linux/man-pages/man7/capabilities.7.html>
- OWASP Docker Security Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html>
- CIS Docker Benchmark and docker-bench-security: <https://www.cisecurity.org/benchmark/docker>, <https://github.com/docker/docker-bench-security>
