Right now every container shares one network, the password sits in `compose.yaml` and the
same three variables are repeated in each service. This stage added
`secrets/db_password.txt` and `config/orders.env`. Put each service only where it must be,
and keep the password out of the file.

1. **Networks.** Two networks: `edge` and `backend`, with `internal: true` on `backend`
   (no route to the outside world). `proxy` is only on `edge`, `api` on both, and `worker`,
   `migrate`, `db` and `cache` only on `backend`.
2. **Ports.** Only `proxy` publishes a port, and only on the loopback address:
   `127.0.0.1:8080:8080`.
3. **The password as a secret.** A top-level `secrets: db_password` read from
   `./secrets/db_password.txt`, given to `db`, `api`, `worker` and `migrate`. `db` reads it
   with `POSTGRES_PASSWORD_FILE=/run/secrets/db_password`; the orders services with
   `DATABASE_PASSWORD_FILE=/run/secrets/db_password`. No `*_PASSWORD` variable is left.
4. **Shared settings.** `api`, `worker` and `migrate` load `config/orders.env` with
   `env_file`, instead of repeating `DATABASE_HOST` and `REDIS_ADDR`.
5. **The build context.** A `.dockerignore` that keeps `secrets/` (and `config/`, `.git` and
   the Markdown) out of `docker build`.

The dojo reads the result through `docker compose config`, which merges the env file into
each service's `environment`, and checks that neither password appears there.

> [!TIP]
> A Compose secret is a file mounted at `/run/secrets/<name>`. Unlike a variable it does
> not show up in `docker inspect`, in `/proc/<pid>/environ` or in a crash dump of the
> environment.
