The image is tiny, but the shortener still runs as root, and the folder now holds things that
must never reach a build: `deploy/production.env` (with the production admin token) and
`docs/`. Give the process the least it needs, and the build only what it needs.

1. Run as a **numeric**, non-root user: `USER 65532:65532` on
   `gcr.io/distroless/static-debian13:nonroot` (the `nonroot` user), or any UID above 0 on
   `scratch`. A name such as `nonroot` works for Docker, but Kubernetes' `runAsNonRoot`
   can only verify a number.
2. Write a `.dockerignore` that keeps `.git`, `deploy/` (or `*.env`), `docs/` and Markdown
   (`*.md`) out of the build context. Leaving the `Dockerfile` out too is a good habit.
3. In the build stage, copy `go.mod` before the sources, so changing `main.go` does not throw
   away the layers above it. If you copy the sources with `COPY . .`, the `.dockerignore` is
   what keeps the secrets out.

The dojo checks the user in the image and in the started container, reads your
`.dockerignore`, and looks for the token in every layer.

> [!TIP]
> The server listens on 8080. A non-root process cannot bind a port below 1024 without the
> `NET_BIND_SERVICE` capability, which is one more reason services listen on high ports
> inside the container and let `-p 80:8080` do the rest.
