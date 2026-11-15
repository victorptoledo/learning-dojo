# Image Hardening: Non-root Users and Distroless Images

Every bug in your service is a way into its container. What the attacker can do once inside
depends on the image: which user they are, which tools they find and which files they can
change. Hardening an image is about making that "inside" as boring as possible.

## Do not run as root

By default a container runs as **root (UID 0)**. Namespaces and dropped capabilities limit
that root, but it is still root: it can rewrite the application, install packages if a
package manager is there, read every file in the image, and it is the starting point for
kernel exploits and for escapes through misconfigurations (a mounted Docker socket, a
privileged flag, a writable host path).

Create a user and switch to it:

```dockerfile
# Debian / Ubuntu based
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home app
# Alpine
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app

USER 10001:10001
```

- Prefer a **numeric** `USER`: Kubernetes' `runAsNonRoot` can only verify numbers, and it
  works even when `/etc/passwd` is missing.
- Many official images ship a user already: `node` in Node.js images, `app` in recent .NET
  images, `65532` (`nonroot`) in distroless `:nonroot` tags.
- Above 1024 there is no need for root to bind a port: listen on 8080, not 80.

## Ownership: write only where needed

A non-root process can only write where its user has permission. Keep the code owned by root
and read-only for the app, and give the app user only the directories it must write:

```dockerfile
COPY --from=build /out/server /app/server          # owned by root, not writable by app
RUN mkdir /data && chown 10001:10001 /data          # before VOLUME, so the volume inherits it
VOLUME /data
USER 10001:10001
```

`COPY --chown=10001:10001` sets ownership while copying, without an extra layer. The
Phase 4 kata is an incident caused by getting this wrong.

## A smaller attack surface

- **Minimal base**: distroless or `scratch` have no shell, no package manager and no
  `curl`, so an attacker who gets code execution has no tools to work with. Alpine and slim
  images are a middle ground.
- **No build tools** in the final stage (multi-stage builds).
- **No secrets** in the image: not in `ENV`, `ARG`, files or layers (Phase 3 shows the build
  secret mounts that replace them).
- **Fewer packages, fewer CVEs**: every package in the image is something a scanner will
  report and someone will have to patch (Phase 3).
- **Read-only at run time**: design the image so it can run with `--read-only`, writing only
  to volumes and tmpfs.

## Health checks without a shell

A `HEALTHCHECK` tells the engine (and Compose) how to test that the service works, not just
that the process exists:

```dockerfile
HEALTHCHECK --interval=10s --timeout=2s --start-period=10s --retries=3 \
    CMD ["/app/server", "-healthcheck"]
```

- The status starts as `starting`, becomes `healthy` after a success and `unhealthy` after
  `--retries` consecutive failures. `docker ps` and `docker inspect` show it; Compose can wait
  for it (`condition: service_healthy`).
- `CMD curl -f http://localhost:8080/health || exit 1` is common but needs `curl` (and a
  shell) in the image. In a distroless image, let the program probe itself with a flag or
  ship a tiny static probe binary.
- `--start-period` gives slow starters time before failures count; `--start-interval`
  probes more often during that period.
- Kubernetes ignores `HEALTHCHECK` and uses its own liveness and readiness probes, but the
  same `/health` endpoint serves both.

## Labels

OCI annotations tell scanners, registries and people where an image came from:

```dockerfile
LABEL org.opencontainers.image.source="https://github.com/acme/orders" \
      org.opencontainers.image.version="1.4.0" \
      org.opencontainers.image.revision="${GIT_SHA}"
```

## A checklist

| Check | Why |
| --- | --- |
| `USER` is non-root and numeric | limits what a compromise can do |
| code not writable by the app user | the attacker cannot patch the app |
| final stage without compilers, shells or `curl` if possible | no tools for the attacker |
| no secrets in `ENV`, `ARG`, files or history | images get pushed and shared |
| `HEALTHCHECK` that needs no extra tools | the engine can tell broken from slow |
| pinned base, scanned regularly | known vulnerabilities get noticed |

## Summary

- Containers run as root by default; set a numeric non-root `USER`.
- Give the app user write access only to its data directories, chowned before `VOLUME`.
- Minimal bases (distroless, scratch) remove the tools an attacker would use.
- `HEALTHCHECK` in exec form, probing with the program itself in shell-less images.
- Label images with their source, version and revision.

## Further reading

- OWASP Docker Security Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html>
- Dockerfile reference, `HEALTHCHECK`: <https://docs.docker.com/reference/dockerfile/#healthcheck>
- Distroless images: <https://github.com/GoogleContainerTools/distroless>
- CIS Docker Benchmark (section 4, container images): <https://www.cisecurity.org/benchmark/docker>
