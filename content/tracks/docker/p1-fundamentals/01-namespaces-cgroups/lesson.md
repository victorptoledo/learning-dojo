# What a Container Is: Namespaces, cgroups and chroot

There is no "container" object in the Linux kernel. A container is an ordinary process that
the runtime starts with three kinds of restrictions: **namespaces** decide what it can
*see*, **cgroups** decide how much it can *use*, and a separate **root file system** decides
which files it finds at `/`. Knowing this explains almost every surprising thing a container
does.

## A container is a process on the host

```powershell
docker run -d --name sleeper alpine:3.24 sleep 1000
docker top sleeper              # the process, as the host sees it (a real host PID)
docker exec sleeper ps          # inside: sleep is PID 1 and there is nothing else
docker rm -f sleeper
```

Inside, `sleep` believes it is process 1 of a tiny machine. On the host (in Docker
Desktop's VM) it is one process among hundreds, with a normal PID. Same process, two views.

## Namespaces: what a process can see

Each namespace type wraps one global resource, so processes inside get their own copy:

| Namespace | Isolates | In a container this means |
| --- | --- | --- |
| PID | process IDs | its first process is PID 1; it cannot see host processes |
| Network (`net`) | interfaces, IP addresses, routes, ports | its own `eth0` and `localhost`; port 8080 inside is not port 8080 on the host |
| Mount (`mnt`) | the list of mounted file systems | its own `/`, volumes mounted only for it |
| UTS | host name | `hostname` returns the container ID or `--hostname` |
| IPC | shared memory, semaphores | no shared memory with other containers |
| User | user and group IDs | root inside can map to an unprivileged user outside (when enabled) |
| Cgroup | the view of the cgroup tree | it sees its own cgroup as the root |

Docker can also *share* namespaces on purpose: `--network host` skips the network
namespace, `--pid host` shows host processes, and `--network container:NAME` joins another
container's network (this is how the dojo's tests reach your service on `localhost`).

## cgroups: what a process can use

**Control groups** (cgroups, version 2 on current systems) group processes and put limits
and accounting on them:

```powershell
docker run --rm --memory 64m --cpus 0.5 --pids-limit 50 alpine:3.24 cat /sys/fs/cgroup/memory.max
# 67108864
```

- **memory**: past the limit the kernel's OOM killer kills a process in the group, and the
  container exits with code 137 (`OOMKilled: true` in `docker inspect`).
- **cpu**: `--cpus 0.5` lets the group use half a core of time; it is throttled, not killed.
- **pids**: caps how many processes it can create, which stops a fork bomb.
- **io**: weights and limits on block devices.

Without limits, one container can starve every other process on the host. Phase 3 covers
limits in depth.

## The root file system: chroot, pivot_root and layers

`chroot` (1979) changes the directory a process sees as `/`. Container runtimes use the
stronger `pivot_root` inside a new mount namespace, so the old root is unmounted and cannot
be reached again. The new root is built from the image's **layers** stacked with
**OverlayFS**: read-only layers from the image, one writable layer for the container on top
(the next lesson).

That is why a container based on Alpine has Alpine's `/bin/sh` and `/etc/os-release` even
when the host runs Debian: only the **user space** comes from the image. `uname -r` inside
prints the **host's** kernel version, because there is only one kernel.

## What still is shared

- **The kernel.** A kernel vulnerability is a way out of every container. For untrusted
  code, add a layer: user namespaces or rootless Docker, or a VM-based runtime such as gVisor
  or Kata Containers.
- **Root is root.** Without a user namespace, UID 0 in the container is UID 0 on the host,
  limited only by dropped capabilities, seccomp and AppArmor or SELinux. Run as non-root
  (Phase 2).
- **The clock**, the kernel's keyring and some `/proc` and `/sys` entries are not
  namespaced.

> [!NOTE]
> The Linux track's lesson "Boot, the Kernel, cgroups and Namespaces" (`linux.p4`) builds a
> container by hand with `unshare` and the cgroup file system. It is a good companion once
> you are comfortable with the shell.

## Summary

- A container is a host process with namespaces (what it sees), cgroups (what it uses) and
  its own root file system (which files it has).
- PID, network, mount, UTS, IPC, user and cgroup namespaces each isolate one resource.
- cgroups v2 limit memory (OOM kill, exit 137), CPU (throttling), processes and I/O.
- The root file system is the image's layers on OverlayFS, entered with `pivot_root`.
- All containers share the host kernel; that is the main difference from a VM.

## Further reading

- namespaces(7), the Linux manual page: <https://man7.org/linux/man-pages/man7/namespaces.7.html>
- cgroups(7): <https://man7.org/linux/man-pages/man7/cgroups.7.html>
- OCI runtime specification (what a runtime must set up): <https://github.com/opencontainers/runtime-spec>
- Docker Engine security overview: <https://docs.docker.com/engine/security/>
