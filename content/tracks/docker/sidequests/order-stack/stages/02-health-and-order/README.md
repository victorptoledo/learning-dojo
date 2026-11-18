`depends_on` with a plain list only waits for a container to *start*: the API comes up before
PostgreSQL accepts connections, and nothing creates the tables. Teach Compose when each
service is really ready, and run the migrations once, in order.

1. **Health checks.** Add `HEALTHCHECK CMD ["/app/orders", "healthcheck", "api"]` to the
   image (exec form: there is no shell), with a short `--interval`. In `compose.yaml`:
   - `db`: `pg_isready -U orders -d orders` (`CMD-SHELL`);
   - `cache`: `["CMD", "redis-cli", "ping"]`;
   - `api`: `["CMD", "/app/orders", "healthcheck", "api"]`;
   - `worker`: `["CMD", "/app/orders", "healthcheck", "worker"]` (the worker has no port;
     it checks its own heartbeat file);
   - `proxy`: `wget -q --spider http://127.0.0.1:8080/healthz` (the nginx image has busybox
     `wget`; `nginx.conf` already answers `/healthz`).
2. **Migrations.** A new `migrate` service: the `orders:2.1.0` image with
   `command: ["migrate"]`, `DATABASE_HOST=db`, started once `db` is healthy, and
   `restart: "no"` (it must exit, not loop).
3. **Start order** with conditions:
   - `api` waits for `db` and `cache` to be `service_healthy` and for `migrate` to be
     `service_completed_successfully`;
   - `worker` waits for a healthy `cache` and finished migrations;
   - `proxy` waits for a healthy `api`.
4. `restart: unless-stopped` on `api`, `worker`, `db`, `cache` and `proxy`.

The dojo waits for your image's health check when it starts the API on its own, and reads
the rest from `docker compose config`. With `docker compose up --build`, watch
`docker compose ps`: `migrate` exits with 0 before `api` starts.

> [!TIP]
> `start_period` gives a slow service time to boot: failed checks during it do not count
> towards `retries`.
