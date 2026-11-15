# Incident: the Image that Will Not Stay Up

The Go service's new image passed code review, but in staging the container exits a second
after it starts, and once that was patched by hand it was marked `unhealthy` and restarted
in a loop. Nobody can see any logs either. You are on call: find every cause and fix the
`Dockerfile`. The service itself (`main.go`) is fine; do not change it.

Investigate the way you would on a real server, with the image built from the starter:

```text
docker build -t broken .
docker run -d --name broken broken
docker ps -a                        # status and exit code
docker logs broken                  # what it printed
docker inspect broken               # State, State.Health.Log, Config
docker run --rm -it --entrypoint sh broken   # look around inside as the same user
```

What the fixed image must do:

| Expectation | How it is checked |
| --- | --- |
| the container keeps running and becomes `healthy` | `State.Status`, `State.Health.Status` |
| the service logs to stdout, so `docker logs` shows `listening on :8080` | the logs |
| it can write to its data directory `/data` (still a volume, still `DATA_DIR=/data`) | the logs, `Config.Env`, `Config.Volumes` |
| `/health` answers `ok` on 8080 | HTTP |
| the server is PID 1 and gets `SIGTERM` on `docker stop`: exec-form `CMD` | `Config.Cmd` is `["server"]` |
| the health check probes the port the server really listens on | `Config.Healthcheck` |
| it still runs as the non-root user `app` (fixing it with root is not a fix) | `Config.User` |

There are 8 tests: 1 in `tests/visible.bats` and 7 hidden ones, plus hadolint.
