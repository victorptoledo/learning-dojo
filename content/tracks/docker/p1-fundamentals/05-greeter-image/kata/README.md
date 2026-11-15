# Greeter Image

`greet.sh` (already written) prints one greeting per argument, using the `GREETING`
environment variable:

```text
GREETING=hello sh greet.sh Ada Grace   ->  hello, Ada!
                                           hello, Grace!
```

Package it so the image works like a command:

```text
docker run --rm greeter             ->  hello, world!
docker run --rm greeter Ada Grace   ->  hello, Ada!
                                        hello, Grace!
docker run --rm -e GREETING=hi greeter Ada   ->  hi, Ada!
```

The team's convention is to copy the whole folder (`COPY . .`), so new scripts are picked up.
The folder also holds `secrets.env` and a `notes/` folder that must **never** reach an image,
and neither should the kata's `tests/`.

| Rule | How it is checked |
| --- | --- |
| start from `alpine:3.24`, work in `/app` | `Config.WorkingDir` |
| `/app/greet.sh` is the exec-form `ENTRYPOINT` | `Config.Entrypoint` is `["/app/greet.sh"]` |
| the default argument is `world` | `Config.Cmd` is `["world"]`, and the output |
| `GREETING` defaults to `hello` | `Config.Env` |
| arguments replace the default | the output of `docker run IMAGE Ada Grace` |
| no `secrets.env`, `notes/` or `tests/` in the image | the list of files in the image |
| a `.dockerignore` keeps them out of the build context | the file exists |

Instructions you will need: `FROM`, `ENV`, `WORKDIR`, `COPY` (with `--chmod`), `ENTRYPOINT`,
`CMD`, and a `.dockerignore` file next to the Dockerfile.

There are 9 tests: 1 in `tests/visible.bats` and 8 hidden ones, plus hadolint.
