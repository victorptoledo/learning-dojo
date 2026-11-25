# Final Exam: Launch Day for the Catalog

`catalog` serves the shop's product catalog. Before it serves, its schema in `DATA_DIR`
must be migrated (`catalog migrate`, safe to run twice); `catalog serve` refuses to start
otherwise. On `SIGTERM` it stops taking new requests and drains the ones in flight for up to
25 seconds. `GET /debug/process` reports its own PID and user.

The image and the Compose file were written in a hurry and the launch is today. In a test run
the container exited at once, `docker logs` showed nothing useful, the health check never
passed, and the planned two replicas fought over port 8080 and over the migration. Fix the
`Dockerfile`, `docker-entrypoint.sh` and `compose.yaml`; do not change `main.go`.

**The image** (built and started once on its own, with a fresh `/data` volume)

| Expectation | How it is checked |
| --- | --- |
| the container keeps running and becomes `healthy` | `State.Status`, `State.Health.Status` |
| the catalog itself is PID 1 (it gets the `SIGTERM`), as the non-root user `10001` | `GET /debug/process`, `Config.User` |
| every log line goes to stdout as JSON (`LOG_FORMAT=json`), and the schema is migrated before the server listens | the logs, `Config.Env` |
| it can write to `/data` (still a volume, still `DATA_DIR=/data`) | the logs, `Config.Env`, `Config.Volumes` |
| exec-form `ENTRYPOINT` running `docker-entrypoint.sh`, `CMD ["serve"]`, so `docker run IMAGE version` prints `2.0.0` | `Config.Entrypoint`, `Config.Cmd`, the output |
| a `HEALTHCHECK` in exec form running `catalog healthcheck`, every 10 s or less, with a `--start-period` | `Config.Healthcheck` |
| `/items` answers, and the image is at most 30 MB | HTTP, `Size` |

**The entrypoint script**: when the command is `serve`, it migrates first unless
`MIGRATE_ON_START=false`; any other command (`migrate`, `version`, `healthcheck`) runs
directly. It stops at the first error and ends by handing over to the catalog.

**compose.yaml** (checked with `docker compose config`, not started)

| Expectation | What the fixed file must say |
| --- | --- |
| a released image | both services use `ghcr.io/dojo-example/catalog:2.0.0` |
| migrations run once, not in every replica | a service `migrate` running the command `migrate`, never restarted; `catalog` sets `MIGRATE_ON_START: "false"` and starts after `migrate` completed successfully |
| two replicas | `catalog` has `deploy.replicas: 2` |
| updates without downtime | `update_config` with `parallelism: 1`, `order: start-first`, `failure_action: rollback` |
| replicas on one host do not fight for a port | `catalog` publishes no fixed host port (for example `ports: ["8080"]`) |
| the drain is not cut short | `stop_grace_period` of at least 30 s; `restart: unless-stopped` |
| the disk does not fill with logs | `json-file` with `max-size: "10m"` and `max-file: "5"` |
| the schema lives in one place | both services mount the named volume `catalog-data` at `/data` |

Investigate first, the way you would on call:

```text
docker build -t catalog .
docker run -d --name cat catalog
docker ps -a
docker logs cat
docker inspect --format '{{json .State}}' cat
docker exec cat ps -o pid,user,args
docker compose config
```

There are 16 tests: 3 in `tests/visible.bats` and 13 hidden ones, plus hadolint and
`compose config`.
