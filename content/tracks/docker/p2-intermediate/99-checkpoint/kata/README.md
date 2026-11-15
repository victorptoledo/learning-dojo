# Checkpoint 2: a Production-ready Image and its Stack

Ship the Go web service (in the kata folder) the way it would go to production: a small,
hardened image, and a `compose.yaml` that runs it next to a Redis cache.

**Dockerfile**

| Rule | How it is checked |
| --- | --- |
| multi-stage, final image at most **30 MB** | the Dockerfile, `Size` |
| no shell in the final image (use distroless) | `/bin/sh` is absent |
| runs as a non-root user | `Config.User` |
| a `HEALTHCHECK` in exec form that runs `/app/server -healthcheck`, every 5 s | `Config.Healthcheck`, and the container becomes `healthy` |
| labels `org.opencontainers.image.title=greeter` and `org.opencontainers.image.version=1.0.0` | `Config.Labels` |
| `PORT=8080`, port 8080 exposed, exec-form entrypoint `/app/server` | `Config` |

**compose.yaml** (checked with `docker compose config`, not started)

| Rule | Checked in the normalised file |
| --- | --- |
| a service `web` built from this folder, publishing `8080:8080` | `build`, `ports` |
| `GREETING_NAME` comes from the environment, `compose` by default | `environment` |
| `web` starts only when `cache` is healthy, and restarts `unless-stopped` | `depends_on`, `restart` |
| a service `cache` from `redis:8.2-alpine` (a pinned tag, never `latest`) | `image` |
| `cache` has a health check running `redis-cli ping` | `healthcheck.test` |
| `cache` keeps its data in a named volume `cache-data` at `/data` | `volumes` |

There are 13 tests: 2 in `tests/visible.bats` and 11 hidden ones, plus hadolint and
`compose config`.
