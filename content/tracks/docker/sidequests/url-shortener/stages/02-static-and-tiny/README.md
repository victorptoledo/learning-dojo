Alpine is small, but the shortener does not use any of it: no shell, no `apk`, no C library.
Each of those is still something to patch, and a shell is the first thing an attacker looks
for. Ship the binary and almost nothing else.

1. Build a **static** binary: `CGO_ENABLED=0`, so it needs no C library. Add
   `-trimpath -ldflags="-s -w"` to drop the build paths and the debug tables.
2. Start the final stage from `scratch` (nothing at all) or from a distroless image such as
   `gcr.io/distroless/static-debian13:nonroot` (CA certificates, time zones and
   `/etc/passwd`, no shell), and copy the binary in. Always name a tag: hadolint rejects an
   untagged or `latest` base.
3. Use an absolute path in the `ENTRYPOINT`: there is no `PATH` lookup worth relying on in
   an image this small.

The dojo checks that the image weighs at most 20 MB, has no shell, no package manager and no
C library, and that everything from stage 1 still works.

> [!TIP]
> Without a shell, `docker exec -it ... sh` no longer works. To look inside, use
> `docker debug` or run a throwaway container that shares the namespaces:
> `docker run -it --rm --pid container:<name> --network container:<name> busybox`.
