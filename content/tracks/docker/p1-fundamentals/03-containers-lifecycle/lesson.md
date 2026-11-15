# The Container Lifecycle: run, exec, stop, PID 1 and Restart Policies

A container lives exactly as long as its **main process**. Everything about starting,
stopping and restarting containers follows from that rule.

## States and the commands that move between them

```text
            create            start               stop / kill / process exits
 image ───────────▶ created ─────────▶ running ─────────────────────────────▶ exited
                                         │  ▲                                    │
                                   pause │  │ unpause                     start  │
                                         ▼  │                                    │
                                        paused            rm ◀──────────────────┘
```

`docker run` is `create` plus `start` (plus `pull` if the image is missing). A stopped
container keeps its writable layer until you `docker rm` it, so `docker start` resumes it with
the files it had. `--rm` removes it automatically on exit.

```powershell
docker run -d --name api -p 8080:8080 -e LOG_LEVEL=debug myapi:1.0
docker ps -a                      # all containers, with status and exit code
docker inspect api --format '{{.State.Status}} {{.State.ExitCode}}'
docker stop api; docker start api; docker restart api
docker rm -f api                  # stop and remove in one step
```

## Configuration at run time

| Flag | Purpose |
| --- | --- |
| `-e KEY=value`, `--env-file app.env` | environment variables (override the image's `ENV`) |
| `-p 8080:80`, `-p 127.0.0.1:8080:80` | publish a port, on every interface or one address |
| `-v`, `--mount` | volumes and bind mounts (lesson 6) |
| `--name`, `--hostname` | names |
| `-w /app`, `-u 10001` | working directory and user |
| `--entrypoint sh` | replace the image's entrypoint, handy for debugging |
| anything after the image name | replaces the image's `CMD` |

## Working inside a running container

```powershell
docker exec -it api sh             # a second process in the same namespaces
docker exec api env                # one command, no terminal
docker logs -f --since 5m api      # stdout and stderr of the main process
docker cp api:/app/config.json .   # copy files out (or in)
docker top api                     # processes, as the host sees them
docker stats api                   # live CPU, memory, network and I/O
```

`exec` starts a new process next to the main one; when the main process exits, the container
stops and takes every `exec` session with it.

## PID 1 and signals

The main process runs as **PID 1** in the container's PID namespace, and PID 1 is special:

- The kernel does **not** apply default signal actions to it. A process that does not handle
  `SIGTERM` ignores it as PID 1, instead of dying.
- It must **reap zombies**: children that exit stay as zombies until PID 1 waits for them.

`docker stop` sends `SIGTERM` (or the image's `STOPSIGNAL`), waits a grace period (10 seconds
by default, `--time` / `-t` to change it), then sends `SIGKILL`. A clean shutdown therefore
needs PID 1 to be your program, and your program to handle `SIGTERM`: finish requests, close
connections, exit.

The **form** of `CMD` and `ENTRYPOINT` decides who PID 1 is:

```dockerfile
CMD node server.js              # shell form: PID 1 is /bin/sh -c "node server.js"
CMD ["node", "server.js"]       # exec form:  PID 1 is node
```

With the shell form, `sh` receives `SIGTERM`, does not forward it, and the server is killed
after 10 seconds without cleaning up. Use the exec form. When your program cannot act as an
init (it starts children, or does not handle signals), add a tiny one: `docker run --init`
or `tini` as the entrypoint. A wrapper script must end with `exec "$@"` so the program
replaces the shell.

## Exit codes

| Code | Usually means |
| --- | --- |
| 0 | the process finished successfully |
| 1, 2, ... | the program's own error |
| 125 | `docker run` itself failed (bad flag, name in use) |
| 126 / 127 | the command cannot run / was not found in the image |
| 137 | killed with `SIGKILL` (128 + 9): `docker kill`, the stop timeout, or an OOM kill |
| 143 | ended by `SIGTERM` (128 + 15) without handling it |

## Restart policies

The engine can restart containers when they exit or when the host reboots:

| Policy | Restarts |
| --- | --- |
| `no` (default) | never |
| `on-failure[:N]` | when the exit code is not 0, at most N times |
| `always` | always, also after `docker stop` once the daemon restarts |
| `unless-stopped` | always, except after you stopped it yourself |

`docker run -d --restart unless-stopped ...` is a good default for long-running services on a
single host. Restarts back off exponentially, so a crash loop does not burn the CPU. In
Kubernetes and other orchestrators, the orchestrator restarts containers instead.

## Summary

- A container runs as long as its main process; `run` = `create` + `start`.
- Stopped containers keep their writable layer until `rm`.
- `exec`, `logs`, `inspect`, `cp`, `top` and `stats` let you look inside.
- `docker stop` sends `SIGTERM`, waits 10 s, then `SIGKILL`; PID 1 must handle it.
- Exec-form `CMD`/`ENTRYPOINT` makes your program PID 1; add `--init` when it cannot be one.
- Restart policies: `no`, `on-failure`, `always`, `unless-stopped`.

## Further reading

- Running containers (Docker docs): <https://docs.docker.com/engine/containers/run/>
- `docker container run` reference: <https://docs.docker.com/reference/cli/docker/container/run/>
- Start containers automatically (restart policies): <https://docs.docker.com/engine/containers/start-containers-automatically/>
- Running more than one process in a container: <https://docs.docker.com/engine/containers/multi-service_container/>
- tini, a minimal init for containers: <https://github.com/krallin/tini>
