# Hardened Compose

`compose.yaml` runs an orders API and its Postgres database. It works, and a security review
sent it back with this list. Fix every finding in `compose.yaml` (there is no Dockerfile in
this kata; the API image is built elsewhere).

| Finding | What the fixed file must say |
| --- | --- |
| floating `latest` tags | `ghcr.io/dojo-example/orders-api:1.4.0` and `postgres:18-alpine` |
| `privileged: true` and the Docker socket mounted into `api` | neither (the socket is root on the host) |
| `api` runs as root with a writable file system | `user: "10001:10001"`, `read_only: true`, a `tmpfs` on `/tmp` |
| `api` keeps every Linux capability | `cap_drop: [ALL]` and `security_opt: [no-new-privileges:true]` |
| no limits | `api` limited to 256M of memory, 0.5 CPU and 200 processes (`deploy.resources.limits`) |
| everything published on every interface | `api` on `127.0.0.1:8080:8080` only, `db` not published at all |
| one flat network | `db` only on a `backend` network marked `internal: true`; `api` on `frontend` and `backend` |
| the password in plain environment variables | a secret `db_password` from `./secrets/db_password.txt`, used by both through `POSTGRES_PASSWORD_FILE` and `DATABASE_PASSWORD_FILE` (`/run/secrets/db_password`) |
| `api` starts before the database is ready | `depends_on` with `condition: service_healthy`, a `pg_isready` health check on `db`, and a health check on `api` (`/app/server -healthcheck`) |
| containers stay down after a crash, logs grow forever | `restart: unless-stopped` on both; `api` logs with `json-file`, `max-size: "10m"`, `max-file: "3"` |

The check runs `docker compose config` on the file (it does not start anything) and reads
the normalised result, so any valid spelling of the same setting passes.

There are 11 tests: 1 in `tests/visible.bats` and 10 hidden ones, plus `compose config`.
