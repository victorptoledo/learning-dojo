# Checkpoint 3: a Release Build with Private Dependencies

`quotes` is a small Go service. In the real project it imports a module from the company's
private Git server, `git.dojo.example`, so its build needs Git credentials (a `.netrc`), and
at run time it reads a partner API token from `/run/secrets/api_token`. The starter builds
and runs, but the release review sent both files back. Fix the `Dockerfile`, add a
`.dockerignore` and fix `compose.yaml`; do not change `main.go`.

Both tokens in the folder (`.env`, `secrets/netrc`, `secrets/api_token.txt`) are fake.

**Dockerfile** (the check builds it *without* any build secret, like a CI job that forgot
to pass one, so a secret mount must not be required)

| Finding | What the fixed image must do | How it is checked |
| --- | --- | --- |
| `FROM` names tags, which can move | every base image is pinned by digest (`image:tag@sha256:...`) | the Dockerfile |
| any change recompiles from scratch | `go.mod` copied before the `.go` files, no `COPY . .`, a cache mount on `/root/.cache/go-build` on the `go build` step | the Dockerfile |
| the Git token is a build argument | the credentials come from a BuildKit secret mount `id=netrc` on the `go build` step; no `GIT_TOKEN` `ARG` or `ENV` | the Dockerfile |
| ... and so it is in the history | no build step mentions the token, no token variable, no `.netrc`, `.env`, `secrets/` or `/src` in the image | `docker history`, `Config.Env`, the files |
| the whole folder goes to the builder | `.dockerignore` lists `.git`, `.env` (or `.env*`) and `secrets/` | the file |
| `-version` prints `dev`, no labels | `ARG VERSION=3.1.0` feeds the binary (`-X main.version=...`) and the label `org.opencontainers.image.version`; labels `title=quotes`, a non-empty `revision` and an `https://` `source` | `docker run IMAGE -version`, `Config.Labels` |
| runs as root | the numeric user `10001`, image at most 30 MB, `/quote` answers | `Config.User`, `Size`, logs, HTTP |
| no health check | `HEALTHCHECK` in exec form running `/app/quotes -healthcheck`; the container becomes `healthy` | `Config.Healthcheck`, the container |

**compose.yaml** (checked with `docker compose config`, not started)

| Finding | What the fixed file must say |
| --- | --- |
| `quotes:latest` | the image `ghcr.io/dojo-example/quotes:3.1.0` |
| the Git token passed as a build argument | `build.args.VERSION: "3.1.0"` and the build secret `netrc` from `./secrets/netrc`; no `GIT_TOKEN` |
| the API token in `environment` and `env_file` | the runtime secret `api_token` from `./secrets/api_token.txt`; no token anywhere in the normalised file |
| root with a writable file system | `user: "10001:10001"`, `read_only: true`, a `tmpfs` on `/tmp` |
| every capability, no limits | `cap_drop: [ALL]`, `no-new-privileges:true`, and limits of `128M` memory, `0.5` CPU and `100` processes |
| published on every interface | `127.0.0.1:8080:8080` only |

Try the build the way CI will, with and without the secret, and compare the history:

```text
docker build -t quotes .
docker build --secret id=netrc,src=secrets/netrc -t quotes .
docker history --no-trunc quotes
docker compose config
```

There are 16 tests: 3 in `tests/visible.bats` and 13 hidden ones, plus hadolint and
`compose config`.
