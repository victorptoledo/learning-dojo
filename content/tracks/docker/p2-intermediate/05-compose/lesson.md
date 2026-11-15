# Docker Compose: Services, Configuration, Secrets and Health Checks

Real applications are several containers: an API, a database, a cache, a worker. Starting
them with a page of `docker run` flags is error-prone. **Docker Compose** describes the whole
application in one `compose.yaml`, following the open **Compose Specification**, and
`docker compose` turns it into containers, networks and volumes.

## A first compose.yaml

```yaml
services:
  api:
    build: .                      # build the Dockerfile in this folder
    image: shop-api:dev           # and tag it
    ports:
      - "127.0.0.1:8080:8080"
    environment:
      DATABASE_HOST: db
    depends_on:
      db:
        condition: service_healthy
    restart: unless-stopped

  db:
    image: postgres:18-alpine
    environment:
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password
    secrets: [db_password]
    volumes:
      - db-data:/var/lib/postgresql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 2s
      retries: 10

volumes:
  db-data:

secrets:
  db_password:
    file: ./secrets/db_password.txt
```

```powershell
docker compose up -d --build      # build if needed, create and start everything
docker compose ps                 # services, state and health
docker compose logs -f api        # logs of one service
docker compose exec db psql -U postgres
docker compose down               # stop and remove containers and the network (volumes stay)
docker compose down -v            # ... and the named volumes
docker compose config             # the file as Compose understands it, fully resolved
```

Compose names everything after the **project** (the folder name, or `-p`, or `name:` at the
top). It creates a default network for the project, and every service is reachable from the
others **by its service name**: the API connects to `db:5432`.

## Configuration: environment and interpolation

- `environment:` sets variables in the container; `env_file:` loads them from a file.
- `${VAR}` anywhere in `compose.yaml` is **interpolated** from your shell or from a `.env`
  file next to it, with defaults: `${TAG:-latest}`, `${API_KEY:?API_KEY is required}`.
- Do not confuse the two: `.env` feeds interpolation of the Compose file; `env_file:` feeds a
  container's environment.

Keep the file in git with safe defaults, and the per-machine values in an **untracked**
`.env`.

## Secrets are not configuration

Environment variables are easy to leak: `docker inspect` shows them, they are inherited by
child processes, crash reporters and debug pages print them. For passwords and tokens, use
**secrets**: Compose mounts each one as a file at `/run/secrets/<name>`, readable by the
service that lists it. Many images support `*_FILE` variables for this (`POSTGRES_PASSWORD_FILE`),
and your own code can read the file. The secret's source (`file:` or `environment:`) stays
out of the image and out of `docker inspect`.

## Start-up order and health checks

`depends_on` controls **order**, not readiness:

| `condition` | `api` starts when `db` ... |
| --- | --- |
| `service_started` (default) | has been started (the database may still be initialising) |
| `service_healthy` | reports `healthy` from its health check |
| `service_completed_successfully` | ran and exited with 0 (migrations, seed jobs) |

So give stateful services a `healthcheck:` and depend on `service_healthy`. Your app should
still retry connections: in production, dependencies come and go.

## Builds, volumes and development loops

- `build:` takes a context, a `dockerfile:`, `args:` and a `target:` (multi-stage).
- Mount the source with a bind mount for development (`./src:/app/src`), or use
  `develop.watch` with `docker compose watch` to sync or rebuild on file changes.
- Named volumes under top-level `volumes:` keep data across `down` and `up`.

## One file, several environments

- **Override files**: `compose.override.yaml` is merged automatically on top of
  `compose.yaml`; `docker compose -f compose.yaml -f compose.ci.yaml up` merges explicitly.
  Typical use: ports and bind mounts only in the override for development.
- **Profiles**: `profiles: [debug]` on a service keeps it off unless you run
  `docker compose --profile debug up`.
- `docker compose config` prints the merged, interpolated result: check it before blaming
  Compose.

Compose is excellent for development, CI and small single-host deployments. Across several
hosts, an orchestrator (Kubernetes, or Swarm) takes over, and the concepts carry over:
services, networks, volumes, secrets and health.

## Summary

- `compose.yaml` describes services, networks, volumes and secrets; `docker compose up`
  creates them under one project.
- Services find each other by service name on the project network.
- `.env` interpolates the file; `environment` and `env_file` configure containers.
- Secrets are files under `/run/secrets`, not environment variables.
- `depends_on` with `service_healthy` waits for a health check; apps should still retry.
- Override files and profiles adapt one file to several environments; `config` shows the
  result.

## Further reading

- Docker Compose documentation: <https://docs.docker.com/compose/>
- Compose file reference: <https://docs.docker.com/reference/compose-file/>
- The Compose Specification: <https://github.com/compose-spec/compose-spec/blob/main/spec.md>
- Secrets in Compose: <https://docs.docker.com/compose/how-tos/use-secrets/>
- Environment variables in Compose: <https://docs.docker.com/compose/how-tos/environment-variables/>
- Control start-up order: <https://docs.docker.com/compose/how-tos/startup-order/>
- Merging Compose files: <https://docs.docker.com/compose/how-tos/multiple-compose-files/merge/>
