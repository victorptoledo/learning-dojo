An online shop takes orders through an API; a worker processes them from a Redis queue,
PostgreSQL keeps them, and nginx sits in front. `main.go` is one binary that plays every
role (read its header comment). Build its image and describe the stack in `compose.yaml`.

**The image** (`Dockerfile`):

1. A build stage on `golang:1.27-alpine` that copies `go.mod`, then the `.go` files, and
   builds a static binary with the compiler cache in a BuildKit cache mount:
   `RUN --mount=type=cache,target=/root/.cache/go-build CGO_ENABLED=0 go build ...`.
2. A final stage on `gcr.io/distroless/static-debian13:nonroot` (or `scratch`) with the binary
   at `/app/orders`, `PORT=8080`, `EXPOSE 8080`, `ENTRYPOINT ["/app/orders"]` and
   `CMD ["api"]`: the same image runs `docker run IMAGE worker`, `migrate` or `version`.

**The stack** (`compose.yaml`), five services:

| Service | What it is |
|---|---|
| `api` | built from this folder (`build: .`) and tagged `image: orders:2.1.0`; `DATABASE_HOST=db`, `REDIS_ADDR=cache:6379` |
| `worker` | the same `orders:2.1.0` image with `command: ["worker"]` and the same two variables; no ports |
| `db` | `postgres:18-alpine` with `POSTGRES_USER=orders`, `POSTGRES_DB=orders`, a password for now (stage 3 moves it to a secret), and the named volume `db-data` on `/var/lib/postgresql` |
| `cache` | a pinned `redis` image (`redis:8-alpine`) started with `redis-server --appendonly yes`, and the named volume `cache-data` on `/data` |
| `proxy` | `nginxinc/nginx-unprivileged:1.29-alpine` with `./proxy/nginx.conf` mounted read-only on `/etc/nginx/conf.d/default.conf`, publishing `8080:8080` |

Every image needs a version tag, never `latest`. The services find each other by name on the
network Compose creates.

The dojo builds and starts your image on its own (no database: `/ready` must answer `503`),
runs `migrate -plan` and `version` with it, and reads `compose.yaml` through
`docker compose config`. It never starts the whole stack; you can, with
`docker compose up --build` and `curl localhost:8080/ready`.
