# Multi-stage Builds and Minimal Runtime Images

To build a service you need compilers, package managers, headers and test tools. To run it
you need almost none of that. A **multi-stage build** keeps both in one Dockerfile and ships
only what the service needs at run time.

## Stages

Every `FROM` starts a new **stage** with its own file system. Only the **last** stage
becomes the image (unless you choose another with `--target`). Earlier stages can be named
and copied from:

```dockerfile
FROM golang:1.27-alpine AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/server ./cmd/server

FROM gcr.io/distroless/static-debian13:nonroot
COPY --from=build /out/server /app/server
EXPOSE 8080
ENTRYPOINT ["/app/server"]
```

The `golang` stage weighs hundreds of megabytes; the final image is a few megabytes of base
plus the binary. Nothing from the build stage reaches the image unless a `COPY --from`
brings it: no compiler, no source code, no module cache.

## More than build and run

Stages can branch. With BuildKit, only the stages the target needs are built, and
independent stages build in parallel:

```dockerfile
FROM node:24-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM deps AS build
COPY . .
RUN npm run build

FROM deps AS test
COPY . .
RUN npm test

FROM node:24-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY --from=build /app/dist ./dist
USER node
CMD ["node", "dist/server.js"]
```

```powershell
docker build --target test .          # CI: run the tests, nothing else
docker build -t shop:1.0 .            # the last stage: the runtime image
```

`COPY --from` also accepts an image: `COPY --from=hadolint/hadolint:v2.15.1 /bin/hadolint
/usr/local/bin/` takes one file from a published image (the dojo's own runner image does
this).

## Choosing a runtime base

| Base | Size (about) | Has | Trade-off |
| --- | --- | --- | --- |
| `debian:trixie-slim`, `python:3.13-slim` | 30–120 MB | glibc, a shell, apt | familiar and compatible; more packages to patch |
| `alpine:3.24` | 8 MB | musl libc, BusyBox shell, apk | small; musl differs from glibc (DNS, performance, prebuilt wheels and binaries) |
| `gcr.io/distroless/...` | 2–50 MB | the language runtime or just CA certificates, time zones, `/etc/passwd`; no shell, no package manager | very small attack surface; debug with `docker debug` or an ephemeral container |
| `scratch` | 0 MB | nothing at all | only for static binaries; you add certificates and a user yourself |

Distroless images come in flavours: `static` (for static binaries), `base` (glibc), `cc`,
`java`, `python3`, `nodejs`, each with a `:nonroot` tag that sets user 65532 and `:debug`
tags that include BusyBox for troubleshooting.

What a minimal image needs that is easy to forget:

- **CA certificates** to call HTTPS APIs (`/etc/ssl/certs/ca-certificates.crt`).
- **Time zone data** if you format times in zones other than UTC.
- **A user** other than root (next lesson), and a writable directory if the app writes.
- A health check that does not need `curl` (the service probing itself, as in the kata).

## Static binaries

Go (with `CGO_ENABLED=0`), Rust (with the `musl` target) and GraalVM native images can
produce binaries with no dynamic library dependencies, which run on `distroless/static` or
`scratch`. A binary linked against glibc needs a glibc base: copying it into Alpine fails
with a confusing `not found` error, because the dynamic loader it names does not exist there.

## Summary

- Each `FROM` starts a stage; only the last (or `--target`) becomes the image.
- `COPY --from=stage` (or an image) brings only the artifacts you choose.
- Stages can share a dependency stage and run tests separately with `--target`.
- Pick the smallest runtime base that has what the program needs: slim, alpine, distroless
  or scratch.
- Do not forget certificates, time zones, a non-root user and a shell-less health check.

## Further reading

- Multi-stage builds: <https://docs.docker.com/build/building/multi-stage/>
- Distroless images: <https://github.com/GoogleContainerTools/distroless>
- Building best practices, "Use multi-stage builds": <https://docs.docker.com/build/building/best-practices/>
