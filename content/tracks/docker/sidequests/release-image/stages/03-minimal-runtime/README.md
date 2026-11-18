The runtime is still Alpine: a shell, `apk` and a C library the binary does not use. Move to
an image that holds only what the API needs. `FROM scratch` is empty, so you also bring the
few things a Go server expects from an operating system.

1. Build a static, stripped binary (`CGO_ENABLED=0`, `-trimpath -ldflags="-s -w"`).
2. Start the final stage from `scratch` (or `gcr.io/distroless/static-debian13:nonroot`,
   which already has most of the list below) and copy in:
   - **CA certificates**, so outgoing TLS works: `/etc/ssl/certs/ca-certificates.crt` from
     the build stage;
   - **time zones**: the Go toolchain ships them as `/usr/local/go/lib/time/zoneinfo.zip`;
     copy it and point `ZONEINFO` at it, or build with `-tags timetzdata` to embed them in
     the binary;
   - a **`/tmp`** anyone can write (`COPY --chmod=1777` of an empty folder made in the build
     stage);
   - the **data folder** owned by 10001 (`COPY --chown=10001:10001`), since there is no
     `RUN mkdir` or `chown` without a shell.
3. Keep `USER 10001:10001`, the environment and the exec-form `ENTRYPOINT` (with an absolute
   path).

`-preflight` now also reports `ca_certs`, `zone` and `tmp_writable`; the dojo checks them,
the size (at most 20 MB) and that no shell or package manager is left.
