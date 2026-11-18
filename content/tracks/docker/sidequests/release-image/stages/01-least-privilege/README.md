The `Dockerfile` in this folder is the one the team ships today. It builds and the API
runs, but it runs as root, with a shell-form `CMD`, and the release checklist rejects it.
Start with the user.

1. Create a group and a user with the fixed id **10001** in the final stage, and run as
   `USER 10001:10001` (numbers, so an orchestrator can verify it is not root).
2. The server writes `stock.json` in `DATA_DIR=/var/lib/inventory` and stops at once when it
   cannot. Create that folder and give it to 10001; nothing else in the image should be
   writable by the process. The binary stays owned by root.
3. Make the binary the exec-form `ENTRYPOINT`, so `docker run IMAGE -preflight` passes the
   flag to it.

`inventory -preflight` prints what the running process can see and do, one `key=value` per
line (`uid`, `gid`, `exe_writable`, `data_writable`, …). The dojo runs it, starts the API and
saves a stock change through `PUT /items/{sku}`.

Try it: `docker build -t inventory .` and `docker run --rm inventory -preflight`.

> [!TIP]
> On Alpine, `addgroup -S -g 10001 inventory && adduser -S -D -H -u 10001 -G inventory inventory`
> creates both without a home folder or a password. The later stages move to an image
> with no shell, where `RUN` is not possible; plan for it.
