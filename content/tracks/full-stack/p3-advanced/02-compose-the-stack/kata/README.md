# Compose the Whole Stack

The shop has three parts: `web` (the built front end, served by a small static server that also
forwards `/api` to the API), `api` (the Node.js server) and `db` (PostgreSQL). `compose.yaml`
runs them on a laptop, but it is not something you could hand to a new teammate or a staging
host: floating tags, a database open to the network, the password in four places, migrations
racing the API's start, and demo data loaded on every start.

Rewrite `compose.yaml` (there is no Dockerfile: the images are built by CI) so that it says:

| Topic | What the file must say |
| --- | --- |
| versions | `web`, `api`, `migrate` and `seed` run the same release: `ghcr.io/dojo-example/shop-web:2.3.0` and `ghcr.io/dojo-example/shop-api:2.3.0`; `db` runs `postgres:18-alpine` |
| one way in | only `web` is published, as `127.0.0.1:8080:8080`; `api` and `db` publish nothing |
| networks | a `frontend` network with `web` and `api`; a `backend` network marked `internal: true` with `api`, `migrate`, `seed` and `db`. So `web` cannot reach `db` at all |
| configuration | `web` gets `API_URL: http://api:3000` (the service name on the Compose network, not `localhost`). `api`, `migrate` and `seed` get `DATABASE_HOST: db`, `DATABASE_NAME: shop`, `DATABASE_USER: shop` and `DATABASE_PASSWORD_FILE: /run/secrets/db_password`; `api` also gets `CORS_ORIGIN: http://localhost:8080` (one origin, never `*`) and `NODE_ENV: production` |
| the secret | a top-level secret `db_password` from `./secrets/db_password.txt`, given to `db`, `api`, `migrate` and `seed`; `db` reads it with `POSTGRES_PASSWORD_FILE`. The password text appears nowhere in `compose.yaml` |
| migrations | a one-shot `migrate` service, `command: ["node", "migrate.js"]`, with `restart: "no"`; `api` only runs `node server.js` |
| start order | `db` has a `pg_isready` health check; `migrate` waits for `db` to be **healthy**; `api` waits for `db` healthy **and** for `migrate` to **complete successfully**; `web` waits for `api` to be **healthy** (`api` has a health check running `["CMD", "node", "healthcheck.js"]`) |
| data | `db` keeps its data in a named volume `db-data` mounted at `/var/lib/postgresql` |
| demo data | `seed` (`command: ["node", "seed.js"]`) only runs with the `dev` profile (`docker compose --profile dev up`), after `migrate` completed successfully |
| restarts | `web`, `api` and `db` restart `unless-stopped` |

The check runs `docker compose config` on the file (nothing starts) and reads the normalised
result, so any valid spelling of the same setting passes.

There are 11 tests: 1 in `tests/visible.bats` and 10 hidden ones, plus `compose config`.
