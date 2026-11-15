# Volumes, Bind Mounts and tmpfs

Containers are meant to be thrown away and recreated from their image. Anything a container
writes to its own file system lives in its **writable layer** and disappears with
`docker rm`. It is also slower (copy-on-write) and grows the container on the host's disk.
Data that must survive goes somewhere else: a **mount**.

## Three kinds of mounts

| Kind | Where the data lives | Managed by | Typical use |
| --- | --- | --- | --- |
| **Named volume** | a directory managed by the engine | Docker (`docker volume ...`) | databases, uploads, any state |
| **Bind mount** | a path you choose on the host | you | source code in development, config files |
| **tmpfs** | memory only | the kernel | scratch files, secrets in memory, a writable `/tmp` on a read-only container |

```powershell
docker volume create pgdata
docker run -d --name db -e POSTGRES_PASSWORD=dev `
  --mount type=volume,source=pgdata,target=/var/lib/postgresql `
  postgres:18-alpine

docker run --rm -it --mount type=bind,source="$PWD",target=/src,readonly alpine:3.24 ls /src
docker run --rm --mount type=tmpfs,target=/tmp,tmpfs-size=64m alpine:3.24 df -h /tmp
```

The short `-v` syntax does the same in one string: `-v pgdata:/var/lib/postgresql` (a name
means a volume), `-v C:\code:/src:ro` (a path means a bind mount). Prefer `--mount`: it is
explicit, and with `-v` a mistyped path silently creates an empty directory.

## Named volumes

- They exist independently of containers: `docker rm db` keeps `pgdata`; `docker volume rm
  pgdata` deletes it, and `docker rm -v` or `docker volume prune` delete unused ones.
- When an **empty** volume is mounted on a directory that has files in the image, Docker
  copies those files into the volume first, **including their owner and mode**. That is how
  a Dockerfile prepares a volume for a non-root user: create the directory and `chown` it
  before `VOLUME` or before the first mount (the Phase 4 kata is about this).
- `docker volume inspect pgdata` shows the driver and mount point. Volume drivers can store
  data on NFS or cloud disks.
- The `VOLUME /data` instruction in a Dockerfile marks a directory that always gets a volume
  (an anonymous one if you do not name it). Anything a later `RUN` writes there is discarded.

## Bind mounts

A bind mount shows a host directory inside the container, live in both directions. It is
perfect for development (edit on the host, the app reloads in the container) and for
injecting a config file read-only. Two caveats:

- **Ownership**: files keep the host's numeric UID and GID, so a non-root process in the
  container may not be allowed to write. Match the user, or mount read-only.
- **Windows**: a path such as `C:\code` lives outside Docker Desktop's Linux VM, so every
  file access crosses the VM boundary: slow for databases and big `node_modules` trees, and
  file change notifications may not arrive. Keep heavy bind mounts inside the WSL 2 file
  system (`\\wsl$\...`) or use a volume.

## Read-only and tmpfs

Making the container's root file system read-only (`--read-only`) and mounting only what must
be writable is a strong hardening step (Phase 3):

```powershell
docker run --read-only --mount type=tmpfs,target=/tmp --mount type=volume,source=data,target=/data myapp:1.0
```

Add `readonly` (or `:ro`) to any mount the container only needs to read.

## Backups

A volume is just data in the engine's storage, so back it up with a short-lived container:

```powershell
docker run --rm --mount source=pgdata,target=/data,readonly -v "${PWD}:/backup" alpine:3.24 `
  tar czf /backup/pgdata.tgz -C /data .
```

For databases, prefer the database's own tool (`pg_dump`, `mysqldump`): a file copy of a
running database may be inconsistent.

## Summary

- The writable layer is temporary, slow and lost with the container.
- Named volumes for state, bind mounts for development and config, tmpfs for scratch.
- An empty volume is filled from the image directory, owner included.
- Bind mounts from Windows are slow and keep host ownership; mount read-only when you can.
- Back up volumes with a temporary container, databases with their dump tools.

## Further reading

- Volumes: <https://docs.docker.com/engine/storage/volumes/>
- Bind mounts: <https://docs.docker.com/engine/storage/bind-mounts/>
- tmpfs mounts: <https://docs.docker.com/engine/storage/tmpfs/>
