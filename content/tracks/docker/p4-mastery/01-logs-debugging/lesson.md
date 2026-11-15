# Logs and Debugging Containers

When a container misbehaves you cannot log in to "the server" and look around: the container
may be gone, restarting, or built without a shell on purpose. Good logging and a repeatable
debugging method make up for that.

## Log the container way

A containerised program writes its logs to **stdout and stderr**, one event per line, and
lets the platform collect them. No log files inside the container, no rotation logic in the
app (see the twelve-factor "logs as event streams" principle).

- The engine captures both streams of the **main process**. Output of processes started by
  `docker exec`, or written to files, does not reach `docker logs`.
- Prefer structured lines (JSON) with a level, a timestamp and request IDs, so a log system
  can filter them.
- Flush promptly: Python needs `PYTHONUNBUFFERED=1`; some runtimes buffer stdout when it is
  not a terminal.
- Official images often symlink log files to the streams (nginx links `access.log` to
  `/dev/stdout`).

```powershell
docker logs api                       # everything
docker logs -f --since 10m --tail 100 api
docker logs -t api 2>&1 | Select-String ERROR     # timestamps; filter in PowerShell
docker compose logs -f api worker
```

## Log drivers and rotation

The **log driver** decides where captured output goes:

| Driver | Where | `docker logs` works |
| --- | --- | --- |
| `json-file` (default) | JSON files on the host | yes |
| `local` | compact files, rotated by default | yes |
| `journald`, `syslog` | the host's logging system | `journald` yes |
| `fluentd`, `gelf`, `awslogs`, `gcplogs`, `splunk` ... | a central system | with dual logging |

**`json-file` does not rotate by default**: a chatty container can fill the host's disk.
Set rotation per container or, better, in the daemon configuration (`daemon.json`):

```json
{ "log-driver": "json-file", "log-opts": { "max-size": "10m", "max-file": "3" } }
```

In Compose: `logging: { driver: json-file, options: { max-size: "10m", max-file: "3" } }`.
In production, ship logs to a central system (Loki, Elasticsearch, a cloud service): when
the container is gone, its local logs are too.

## A debugging method

**1. What state is it in?**

```powershell
docker ps -a --filter name=api
docker inspect api --format '{{json .State}}'
```

`State` holds `Status`, `ExitCode`, `OOMKilled`, `Error`, `RestartCount` and, with a health
check, `Health.Log` with the output of the last probes.

**2. What did it say?** `docker logs --tail 50 api`. The **first** error is usually the cause;
later ones are consequences.

**3. Read the exit code.** 0 finished; 1 or another small number: the application's error;
126 or 127: the command cannot run or does not exist in the image (wrong `CMD`, missing
shell, a glibc binary on Alpine); 137: `SIGKILL`, often an OOM kill (check `OOMKilled`);
143: `SIGTERM` not handled.

**4. What happened around it?** `docker events --since 30m --filter container=api` shows
create, start, die, oom, health status changes and kills, with times.

**5. Look inside.**

- A running container with a shell: `docker exec -it api sh`.
- A container that crashes at start: run the **image** with another entrypoint and look
  around as the same user: `docker run --rm -it --entrypoint sh shop-api:1.4.0`.
- A container that exited: `docker cp api:/app/config.yaml .` still works on stopped
  containers; `docker diff api` shows what it changed in its writable layer.
- A shell-less (distroless) container: `docker debug api` attaches a toolbox shell to it
  without changing the image (Docker Desktop); or start a tool container that **shares its
  namespaces**:

```powershell
docker run --rm -it --network container:api --pid container:api nicolaka/netshoot
# ss -ltnp, curl localhost:8080/health, ps aux, dig db, tcpdump -i eth0
```

**6. Resources.** `docker stats` for live CPU, memory and I/O against the limits; `docker
system df -v` for disk used by images, containers, volumes and the build cache.

## Common causes, quickly

| Symptom | Look at |
| --- | --- |
| exits at once, exit 1, "permission denied" in logs | the `USER` versus owners of the paths it writes |
| exit 127, "exec format error" or "not found" | the `CMD`, the binary's architecture or C library |
| restart loop, exit 137, `OOMKilled: true` | the memory limit versus the runtime's heap settings |
| `unhealthy` but logs look fine | `Health.Log`: wrong port, missing `curl`, timeout too short |
| `docker logs` is empty | logs written to a file, or buffered stdout |
| slow stop, then exit 137 | shell-form `CMD`: PID 1 ignores `SIGTERM` |
| works locally, not from other containers | listening on 127.0.0.1, wrong network, wrong service name |

## Summary

- Log to stdout and stderr as structured lines; the engine and your platform collect them.
- `json-file` does not rotate unless you set `max-size` and `max-file`; centralise logs in
  production.
- Method: state (`inspect`), logs (first error), exit code, events, inside, resources.
- Debug crashing images with `--entrypoint sh`, shell-less ones with `docker debug` or a
  tool container sharing their namespaces.

## Further reading

- Configure logging drivers: <https://docs.docker.com/engine/logging/configure/>
- The json-file driver and its rotation options: <https://docs.docker.com/engine/logging/drivers/json-file/>
- `docker container logs`: <https://docs.docker.com/reference/cli/docker/container/logs/>
- `docker debug`: <https://docs.docker.com/reference/cli/docker/debug/>
- `docker inspect`: <https://docs.docker.com/reference/cli/docker/inspect/>
- `docker container stats`: <https://docs.docker.com/reference/cli/docker/container/stats/>
- `docker system df`: <https://docs.docker.com/reference/cli/docker/system/df/>
- The Twelve-Factor App, "Logs": <https://12factor.net/>
