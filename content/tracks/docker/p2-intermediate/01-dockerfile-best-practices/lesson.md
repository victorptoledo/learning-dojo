# Dockerfile Best Practices: Cache Order, Small Layers and Linting

A Dockerfile that works is the start. A good one rebuilds in seconds after a code change,
produces a small image, gives the same result next month and passes a linter. Most of that
comes from understanding the **build cache**.

## How the build cache works

The builder walks the Dockerfile from the top. For each instruction it asks: *do I already
have a layer built from the same parent with the same instruction?*

- For `RUN`, the key is the **instruction text** (and the build arguments it uses). The
  builder does not look at what the command would download: `RUN apt-get update` stays cached
  for months.
- For `COPY` and `ADD`, the key includes a **checksum of the files** being copied (contents
  and metadata, not timestamps).
- As soon as one step misses the cache, **every step after it is rebuilt**.

So the order of instructions is a performance decision: things that change rarely go first,
things that change often go last.

```dockerfile
# Slow: any change to any file reruns the install
COPY . .
RUN npm ci

# Fast: the install is cached until the manifests change
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
```

The same pattern works everywhere: `go.mod`/`go.sum` then `go mod download`; `pom.xml` then
`mvn dependency:go-offline`; `requirements.txt` then `pip install`; the `.csproj` then
`dotnet restore`.

## Small layers

- **Combine** steps that create and delete files, so the deleted files never land in a layer:

  ```dockerfile
  RUN apt-get update \
      && apt-get install -y --no-install-recommends libpq5 ca-certificates \
      && rm -rf /var/lib/apt/lists/*
  ```

  `apt-get update` and `install` must be in the **same** `RUN`: an `update` cached on its
  own would make a later `install` use stale package lists.
- Skip what you do not need: `--no-install-recommends` (apt), `apk add --no-cache` (Alpine),
  `pip install --no-cache-dir`, `npm ci --omit=dev`.
- Start from a **small base**: `-slim` and `-alpine` variants, or distroless (next lessons).
  The base is usually most of the image.
- Keep build tools out of the final image with a multi-stage build (next lesson).

## Reproducible builds

- **Pin the base image**: `FROM python:3.13-slim`, never a bare `FROM python`. For critical
  images, pin the digest too: `FROM python:3.13-slim@sha256:...`, and let a bot (Renovate,
  Dependabot) propose updates.
- **Pin package versions** where it matters (`apk add curl=8.14.1-r0`), and always lock
  application dependencies (lock files, `go.sum`).
- Prefer `COPY` over `ADD`. If you download in a build, verify a checksum
  (`ADD --checksum=sha256:...` or `sha256sum -c`).
- Keep the build context small with `.dockerignore`, so the checksums of `COPY . .` only
  cover what matters.

## Readable and lint-clean

- One concern per `RUN`, with `&&` and `\` line breaks, packages sorted alphabetically.
- `SHELL ["/bin/bash", "-o", "pipefail", "-c"]` before a `RUN` with a pipe, so a failing
  `curl | tar` fails the build instead of producing a broken layer.
- `WORKDIR` instead of `cd`; exec form for `CMD` and `ENTRYPOINT`; labels from the OCI
  annotations (`org.opencontainers.image.source`, `.version`, `.revision`).
- Run **hadolint** in CI. Rules you will meet:

| Rule | Says |
| --- | --- |
| DL3006 / DL3007 | tag your base image / do not use `latest` |
| DL3008 / DL3018 | pin versions in `apt-get install` / `apk add` |
| DL3009 | delete the apt lists after installing |
| DL3025 | use the JSON (exec) form for `CMD` and `ENTRYPOINT` |
| DL3003 | use `WORKDIR` to switch directories |
| DL4006 | set `pipefail` before using pipes in `RUN` |
| SC2086 and other `SC` rules | ShellCheck findings inside `RUN` |

## Summary

- A cache miss rebuilds every later step: order instructions from stable to volatile.
- Copy dependency manifests and install them before copying the code.
- Create and clean up in one `RUN`; `apt-get update` and `install` together.
- Pin base images (tag, and digest for critical builds) and dependency versions.
- Keep the context small, use `pipefail`, and lint with hadolint.

## Further reading

- Building best practices (Docker docs): <https://docs.docker.com/build/building/best-practices/>
- Docker build cache: <https://docs.docker.com/build/cache/>
- Optimize cache usage in builds: <https://docs.docker.com/build/cache/optimize/>
- hadolint rules: <https://github.com/hadolint/hadolint>
