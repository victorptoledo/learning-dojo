# Hello, Image

Write a `Dockerfile` that packages `hello.txt` (already in the kata folder) and prints it
when a container starts:

```text
docker build -t hello-dojo .
docker run --rm hello-dojo     ->  prints the content of hello.txt and exits with status 0
```

The image must:

| Rule | How it is checked |
| --- | --- |
| start from `alpine:3.24` | the file `/etc/alpine-release` is in the image |
| keep the file at `/srv/hello.txt` | the path is in the image |
| use `/srv` as its working directory | `Config.WorkingDir` of the image |
| carry the label `org.opencontainers.image.title=hello-dojo` | `Config.Labels` |
| print the file with an **exec-form** `CMD` (a JSON array) | the output, and `Config.Cmd` does not start with `/bin/sh -c` |

What you start with is a Dockerfile that builds but only says "todo".

Instructions you will need: `FROM`, `LABEL`, `WORKDIR`, `COPY`, `CMD`.

How the check works: the dojo runs `docker build --network none` on your kata folder, starts
the image once with no network, and gives the tests what it saw (the image config, the list
of files in the image, what the container printed). It also runs **hadolint**, a Dockerfile
linter, as one more test. The image is removed afterwards.

There are 6 tests: 1 in `tests/visible.bats` and 5 hidden ones, plus hadolint.
