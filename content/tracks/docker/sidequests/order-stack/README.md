# Sidequest: Order stack

An online shop's order service has to run next to its database, its queue and a reverse
proxy. The program is written (`main.go`, one Go binary for the API, the worker and the
migrations); you build its image and the Compose stack around it, in four stages:

1. one image for every role, and the five services: `api`, `worker`, `db`, `cache`, `proxy`;
2. health checks, a one-shot `migrate` service and a start order that waits for each
   dependency to be ready, not just started;
3. an internal network that only the proxy and the API leave, a single published port on the
   loopback address, the database password as a secret and shared settings in an env file;
4. hardening: non-root users, read-only file systems, no capabilities, memory limits, log
   rotation and two workers.

How it is checked:

- Your work is the `Dockerfile`, the `.dockerignore` and `compose.yaml` at the top of this
  folder. You never need to edit `main.go` or `proxy/nginx.conf`.
- The dojo builds the image on your Docker engine with no network for `RUN` steps, starts it
  once on its own (as the API, with no database) and runs it with a few arguments. Then the
  `dojo-docker` image checks the `Dockerfile` with hadolint and reads `compose.yaml` through
  `docker compose config`, so anchors, env files and defaults are resolved the way Compose
  resolves them.
- The whole stack is never started by the check. Start it yourself with
  `docker compose up --build`; it needs network access to pull PostgreSQL, Redis and nginx.

When you are done, `[A]` copies a review prompt: ask an AI about your stack.
