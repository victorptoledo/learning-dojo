The stack starts in order and keeps its secrets, but any service that is broken into can
write anywhere in its file system, use every default capability, eat all the memory of the
host and fill the disk with logs. Make it fit to leave running.

1. **The image** runs as `USER 10001:10001`, the user the services below set.
2. **`api`, `worker` and `migrate`:** `user: "10001:10001"`, `read_only: true`, a `tmpfs` on
   `/tmp` (the worker writes its heartbeat there), `cap_drop: [ALL]` and
   `security_opt: [no-new-privileges:true]`. Write it once as an extension field
   (`x-hardened: &hardened`) and merge it with `<<: *hardened`.
3. **`proxy`:** the unprivileged nginx runs as a normal user on port 8080, so it also takes
   `read_only: true`, a `tmpfs` on `/tmp` and `cap_drop: [ALL]`.
4. **`db` and `cache`** start as root to fix the ownership of their data, so they keep their
   capabilities, but every service gets `no-new-privileges:true`.
5. **Limits:** every service has `deploy.resources.limits.memory`; `api` gets
   `cpus: "0.50"` and `memory: 256M`.
6. **Logs:** `api`, `worker`, `db`, `cache` and `proxy` use the `json-file` driver with
   `max-size: "10m"` and `max-file: "3"` (an `x-logging` anchor again).
7. **Two workers:** `deploy.replicas: 2` on `worker`. Compose names the containers itself;
   a fixed `container_name` on any service would make replicas impossible.

The dojo reads the result through `docker compose config`, where the anchors are already
merged into each service. Run the stack with `docker compose up --build` and
`docker compose ps`, then try `docker compose exec api touch /app/x`: it must fail.
