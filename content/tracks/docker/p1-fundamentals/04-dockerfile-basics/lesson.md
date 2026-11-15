# Dockerfile Instructions, the Build Context and .dockerignore

A Dockerfile is a recipe the builder runs from top to bottom: start from a base image, change
files, and record how the result should start. Each instruction either adds a layer or
changes the image config.

## The build context

```powershell
docker build -t greeter:1.0 .
```

The final `.` is the **build context**: the folder whose files the CLI sends to the builder.
`COPY` and `ADD` can only read files from the context, never from elsewhere on your disk.
Everything in it is sent (to a builder that may even be remote), so a context with `.git`,
`node_modules` or a 2 GB data file makes every build slow, and a context with `.env` or keys
can leak them into the image with one careless `COPY . .`.

A **`.dockerignore`** next to the Dockerfile excludes paths, with the same idea as
`.gitignore`:

```text
.git
**/node_modules
*.md
.env
secrets/
Dockerfile
.dockerignore
```

## The instructions

| Instruction | What it does | Adds a layer |
| --- | --- | --- |
| `FROM image:tag [AS name]` | the base image (and starts a build stage) | — |
| `RUN command` | runs a command at **build** time, in a shell by default | yes |
| `COPY src... dest` | copies files from the context (or another stage) | yes |
| `ADD src dest` | like `COPY`, also fetches URLs and unpacks local tar files | yes |
| `WORKDIR /app` | sets (and creates) the working directory for later instructions and the container | — |
| `ENV KEY=value` | an environment variable at build time **and** in containers | — |
| `ARG NAME=default` | a variable at build time only, set with `--build-arg` | — |
| `EXPOSE 8080` | documents a port; it does not publish it | — |
| `USER 10001` | the user for later `RUN` steps and for the container | — |
| `LABEL key="value"` | metadata, such as `org.opencontainers.image.source` | — |
| `ENTRYPOINT [...]` | the executable the container runs | — |
| `CMD [...]` | default arguments (or the default command when there is no entrypoint) | — |
| `HEALTHCHECK`, `STOPSIGNAL`, `VOLUME`, `SHELL`, `ONBUILD` | covered later in the track | — |

Some details that trip people up:

- Each `RUN` runs in a **new shell**: `RUN cd /app` changes nothing for the next line. Use
  `WORKDIR`.
- Prefer `COPY`. Use `ADD` only to unpack a local archive or fetch a URL with a checksum
  (`ADD --checksum=sha256:... https://...`).
- `ENV` values are stored in the image config, where anyone with the image can read them.
  Never put secrets in `ENV` or `ARG` (Phase 3).
- `COPY --chown=10001:10001` and `COPY --chmod=755` set ownership and modes as files are
  copied, without an extra `RUN chmod` layer. Files copied from Windows often arrive without
  the executable bit, so set it explicitly for scripts.

## Shell form and exec form

`RUN`, `CMD` and `ENTRYPOINT` accept two forms:

```dockerfile
RUN apk add --no-cache curl          # shell form: /bin/sh -c "apk add --no-cache curl"
CMD ["server", "--port", "8080"]     # exec form: a JSON array, run directly
```

The shell form gives you variables, pipes and `&&`, which is useful in `RUN`. For `CMD` and
`ENTRYPOINT`, use the **exec form**, so your program is PID 1 and receives signals (previous
lesson), and so the image works even without a shell (distroless, Phase 2). The exec form
does no variable expansion: `CMD ["echo", "$HOME"]` prints `$HOME`.

## ENTRYPOINT and CMD together

The container runs **ENTRYPOINT followed by CMD**. Arguments after the image name in
`docker run` replace **CMD only**:

```dockerfile
ENTRYPOINT ["/app/greet.sh"]
CMD ["world"]
```

| Command | Runs |
| --- | --- |
| `docker run greeter` | `/app/greet.sh world` |
| `docker run greeter Ada Grace` | `/app/greet.sh Ada Grace` |
| `docker run --entrypoint sh greeter -c "ls /app"` | `sh -c "ls /app"` |

Use `ENTRYPOINT` for an image that *is* one program (a CLI, a server) and `CMD` for its
default arguments. An image with only `CMD` lets users replace the whole command, which
suits base and tool images.

## A complete small example

```dockerfile
FROM alpine:3.24
LABEL org.opencontainers.image.title="greeter"
ENV GREETING=hello
WORKDIR /app
COPY --chmod=755 greet.sh .
USER 10001
ENTRYPOINT ["/app/greet.sh"]
CMD ["world"]
```

Lint every Dockerfile with **hadolint** (`docker run --rm -i hadolint/hadolint < Dockerfile`):
it catches shell-form commands, unpinned images, `cd` in `RUN` and many shell mistakes, and
the dojo runs it on every kata.

## Summary

- The build context is the folder sent to the builder; `.dockerignore` keeps it small and
  keeps secrets out.
- `RUN`, `COPY` and `ADD` add layers; the other instructions change the config.
- `WORKDIR`, not `RUN cd`; `COPY`, not `ADD`; never secrets in `ENV` or `ARG`.
- Exec form for `CMD` and `ENTRYPOINT`; the container runs ENTRYPOINT + CMD, and `docker run`
  arguments replace CMD.

## Further reading

- Dockerfile reference: <https://docs.docker.com/reference/dockerfile/>
- Build context and `.dockerignore`: <https://docs.docker.com/build/building/context/>
- hadolint, the Dockerfile linter: <https://github.com/hadolint/hadolint>
