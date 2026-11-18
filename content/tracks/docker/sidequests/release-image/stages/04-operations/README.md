The image is small and locked down; now make it easy to run and to debug when it is one of
fifty containers on a host at three in the morning.

1. **PID 1.** The binary must be the container's first process, so `docker stop` sends
   SIGTERM to it and it shuts down cleanly. An exec-form `ENTRYPOINT` does that; a shell in
   between would swallow the signal (and there is no shell any more anyway).
2. **Health.** `HEALTHCHECK CMD ["/inventory", "-healthcheck"]` with a short `--interval`.
3. **Version.** `ARG VERSION=2.3.0`, passed to the build with
   `-ldflags="... -X main.version=${VERSION}"`: `-version` prints `inventory 2.3.0` and the
   first log line carries it.
4. **Data.** Declare `/var/lib/inventory` as a `VOLUME`, so the stock outlives the container
   and a new release finds it. The folder is copied with its owner, so the volume Docker
   creates from it is writable by 10001 too.

The logs are JSON lines on standard output, which `docker logs` and every log shipper read
as they are; the dojo checks the first line. Look at a running container with
`docker inspect --format '{{json .State.Health}}' <name>` and `docker logs <name>`.
