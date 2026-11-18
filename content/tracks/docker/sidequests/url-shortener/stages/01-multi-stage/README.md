The `Dockerfile` in this folder works: build it and the shortener runs. But the image is the
whole `golang:1.27` toolchain image plus the sources, close to 900 MB, to run one binary of a
few megabytes. Split the build from the runtime.

1. Name a build stage (`FROM golang:1.27-alpine AS build`), copy `go.mod` and the `.go`
   files, and build the binary there.
2. Start a second stage from a small base (`alpine:3.24` is fine for now) and copy **only the
   binary** from the build stage with `COPY --from=build`.
3. Keep `PORT=8080`, `EXPOSE 8080` and an exec-form start command (`ENTRYPOINT` or `CMD`).

The dojo checks that the final image has no `/usr/local/go` and no source files, weighs at
most 30 MB, answers `/health`, and that a link posted to `/links` redirects with a `302`.

Compare the sizes yourself:

```
docker build -t shortener .
docker image ls shortener
```

> [!TIP]
> Only the last stage becomes the image. Everything a build stage downloads or compiles stays
> in the build cache, ready for the next build, but never reaches the image you ship.
