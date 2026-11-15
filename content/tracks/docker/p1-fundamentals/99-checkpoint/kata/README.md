# Checkpoint 1: Containerise a Web Service

The kata folder holds a small Go web service (`main.go`, `go.mod`, standard library only).
Write a `Dockerfile` that builds it and runs it, the way a team would ship a first version:

```text
docker build -t greeter .
docker run --rm -p 8080:8080 greeter
curl localhost:8080/health   ->  ok
curl localhost:8080/hello    ->  hello, dojo
```

The service reads `PORT` (where it listens) and `GREETING_NAME` (who `/hello` greets) from
the environment, and logs one JSON line per request to stdout.

| Rule | How it is checked |
| --- | --- |
| build with `golang:1.27-alpine` in `/src` | `Config.WorkingDir`, `/usr/local/go` in the image |
| copy `go.mod` **before** the `.go` files, so changing code does not invalidate the module layer | the order of the build steps (`docker history`) |
| the binary is `/usr/local/bin/server` | the path is in the image |
| `PORT=8080` and `GREETING_NAME=dojo` are set in the image | `Config.Env` |
| port 8080 is documented with `EXPOSE` | `Config.ExposedPorts` |
| the server is PID 1, started with an exec-form `CMD` | `Config.Cmd` is `["server"]` |
| `README.md` and `tests/` stay out of the image | the files in the image |
| the container keeps running and answers on 8080 | `GET /health`, `GET /hello`, the logs |

The build runs with no network, which is fine: the service has no dependencies to download.
Single stage is expected here; Phase 2 makes the image small.

There are 10 tests: 1 in `tests/visible.bats` and 9 hidden ones, plus hadolint.
