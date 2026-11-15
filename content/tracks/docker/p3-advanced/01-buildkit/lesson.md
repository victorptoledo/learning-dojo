# BuildKit: Cache Mounts, Secret Mounts, Build Arguments and Multi-platform Builds

**BuildKit** is the builder behind `docker build` since Docker 23 (and `docker buildx`). It
reads the Dockerfile as a graph, not a script: stages that do not depend on each other build
in parallel, stages the target does not need are skipped, and `RUN` gets new mount types
that change how you deal with caches and secrets.

## Cache mounts: keep the package cache, not in the image

Layer caching helps only while the inputs are unchanged. Change one dependency and the whole
install step reruns, downloading everything again. A **cache mount** gives a `RUN` step a
directory that persists **between builds on the builder**, but is never part of a layer:

```dockerfile
RUN --mount=type=cache,target=/root/.cache/go-build \
    --mount=type=cache,target=/go/pkg/mod \
    go build -o /out/server .

RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests
RUN --mount=type=cache,target=/root/.npm npm ci
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt
RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    apt-get update && apt-get install -y --no-install-recommends build-essential
```

The image stays as small as with `--no-cache-dir`, and rebuilds download or compile only
what changed. `sharing=locked` serialises steps that must not share a cache concurrently
(apt). `docker builder prune` clears these caches.

## Secret mounts: credentials during the build only

Builds sometimes need a token: a private package registry, a licensed download. Putting it
in `ARG` or `ENV` records it in the image config and history; copying a file records it in a
layer. A **secret mount** exposes it as a file for one `RUN` step and leaves no trace:

```dockerfile
RUN --mount=type=secret,id=npm_token,env=NPM_TOKEN \
    npm ci
RUN --mount=type=secret,id=pip_conf,target=/etc/pip.conf \
    pip install -r requirements.txt
```

```powershell
docker build --secret id=npm_token,env=NPM_TOKEN --secret id=pip_conf,src=pip.conf .
```

For private git repositories, `RUN --mount=type=ssh` forwards your SSH agent
(`docker build --ssh default`) instead of copying keys.

## Build arguments and their scope

`ARG` defines a variable for the build, set with `--build-arg NAME=value`:

```dockerfile
ARG VERSION=dev                # global: usable only in FROM lines

FROM golang:1.27-alpine AS build
ARG VERSION                    # declare again to use it in this stage
RUN go build -ldflags "-X main.version=${VERSION}" -o /out/server .

FROM gcr.io/distroless/static-debian13:nonroot
ARG VERSION                    # and again in this one
LABEL org.opencontainers.image.version="${VERSION}"
```

- An `ARG` before the first `FROM` is visible only to `FROM` lines. Each stage that uses it
  must declare it again (without a value, it inherits the global default).
- Changing a build argument used by a `RUN` invalidates that step's cache.
- BuildKit predefines `TARGETPLATFORM`, `TARGETOS`, `TARGETARCH`, `BUILDPLATFORM` for
  multi-platform builds.
- Build arguments are visible in `docker history`: never secrets.

## Caches in CI

CI runners usually start with an empty builder, so the layer cache is gone on every run.
Export it and import it again:

```powershell
docker buildx build `
  --cache-from type=registry,ref=ghcr.io/acme/shop:buildcache `
  --cache-to type=registry,ref=ghcr.io/acme/shop:buildcache,mode=max `
  -t ghcr.io/acme/shop:1.4.0 --push .
```

Backends include `registry`, `inline`, `local`, and CI-specific ones such as `gha` for
GitHub Actions. `mode=max` also caches intermediate stages, not just the final one.

## Multi-platform images

ARM servers and Apple silicon laptops need `linux/arm64` images; most servers are
`linux/amd64`. `buildx` builds both and pushes one **index** that points at each:

```powershell
docker buildx create --use --name multi
docker buildx build --platform linux/amd64,linux/arm64 -t ghcr.io/acme/shop:1.4.0 --push .
```

Foreign platforms are built under QEMU emulation (slow for compilation) or on native remote
builders. Faster still for compiled languages: run the compiler on the build platform and
cross-compile:

```dockerfile
FROM --platform=$BUILDPLATFORM golang:1.27-alpine AS build
ARG TARGETOS TARGETARCH
RUN GOOS=$TARGETOS GOARCH=$TARGETARCH CGO_ENABLED=0 go build -o /out/server .
```

## Other things BuildKit does

- **Here-documents** in `RUN` and `COPY` (`RUN <<EOF ... EOF`) for readable multi-line
  scripts.
- `COPY --link` creates layers independent of the previous ones, so rebasing onto a new
  base image does not rebuild them.
- **Exporters** write the result somewhere other than the image store: `--output
  type=local,dest=out` extracts files (useful to build binaries in a container),
  `type=oci` writes an OCI archive.
- **Attestations**: `--sbom=true` and `--provenance=true` attach a software bill of
  materials and build provenance to the image (next lesson).

## Summary

- BuildKit builds stages as a graph: in parallel, and only what the target needs.
- `--mount=type=cache` keeps package and compiler caches on the builder, out of the image.
- `--mount=type=secret` and `type=ssh` give credentials to one step without leaving traces;
  `ARG` and `ENV` are never secret.
- Redeclare `ARG` in each stage that uses it.
- Export and import the cache in CI; build `linux/amd64` and `linux/arm64` with `buildx`,
  cross-compiling when you can.

## Further reading

- BuildKit overview: <https://docs.docker.com/build/buildkit/>
- Build secrets: <https://docs.docker.com/build/building/secrets/>
- Cache storage backends: <https://docs.docker.com/build/cache/backends/>
- Multi-platform builds: <https://docs.docker.com/build/building/multi-platform/>
- Exporters: <https://docs.docker.com/build/exporters/>
- Introduction to heredocs in Dockerfiles: <https://www.docker.com/blog/introduction-to-heredocs-in-dockerfiles/>
