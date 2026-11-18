Write the `Dockerfile` that turns this folder into a running message board. `main.go` is the
whole program (read its header comment); you only package it.

1. Start from `golang:1.27-alpine` and work in `/src`.
2. Copy `go.mod` and the `.go` files, and build the binary to `/usr/local/bin/guestbook`
   (`go build -o /usr/local/bin/guestbook .`).
3. Set `PORT=8080` and `BOARD_TITLE="Dojo board"` with `ENV`, and document the port with
   `EXPOSE`.
4. Start the server with an exec-form `CMD` (a JSON array), so the server is the container's
   main process and gets the stop signal.

The dojo builds your image, starts it once with no network, and then asks it for `/health`
and `/`, which must show the title on its first line. The server must keep running and log
`listening on :8080` to standard output, where `docker logs` reads it.

Try it yourself:

```
docker build -t guestbook .
docker run --rm -p 8080:8080 guestbook
```

and open <http://localhost:8080/> (or `curl localhost:8080/messages`).

> [!TIP]
> `docker build` reruns every step after the first one whose input changed. Copying `go.mod`
> on its own line is not needed yet, but it starts a good habit for stage 2.
