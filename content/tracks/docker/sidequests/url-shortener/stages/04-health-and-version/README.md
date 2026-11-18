The image is small and safe, but nobody can tell which version it runs, and Docker cannot
tell whether it works. Add a health check that needs no shell, and stamp one version into the
binary and the image.

1. Add a `HEALTHCHECK` in exec form that runs the binary's own probe:
   `CMD ["/shortener", "-healthcheck"]`. There is no `curl` or `wget` in the image, and no
   shell for `CMD-SHELL`. Pick a short `--interval` (5 or 10 s), so a new container turns
   healthy quickly.
2. Declare `ARG VERSION=1.0.0` and pass it to the build:
   `-ldflags="-s -w -X main.version=${VERSION}"`. Then `docker run IMAGE -version` prints
   `shortener 1.0.0`, and the server logs it when it starts.
3. Add the labels `org.opencontainers.image.title="shortener"`,
   `org.opencontainers.image.version` (the same version) and
   `org.opencontainers.image.source` (an `https://` URL of the code).

An `ARG` is visible only in the stage that declares it: declare it again after the second
`FROM` to use it in a label.

The dojo waits for the health check, runs `-version`, reads the labels, and checks that every
earlier stage still holds.

Try another version: `docker build --build-arg VERSION=1.1.0 -t shortener .`, then
`docker inspect --format '{{json .Config.Labels}}' shortener` and
`docker inspect --format '{{.State.Health.Status}}' <container>`.
