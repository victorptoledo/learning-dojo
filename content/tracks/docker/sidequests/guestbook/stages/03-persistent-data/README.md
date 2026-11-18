Every message lives in the container's writable layer: it is gone when the container is
removed, and a new version of the image starts with an empty board. Give the data a place of
its own, and greet every new board with a first message.

1. Set `DATA_DIR=/data` and declare `/data` as a `VOLUME`. The server creates
   `/data/messages.txt` there.
2. Copy `welcome.txt` (new in this stage) to `/usr/share/guestbook/welcome.txt`. When the
   board has no messages file yet, the server starts it with that text, so a fresh board
   shows the welcome message first.
3. Keep everything else: the labels, the `ENTRYPOINT` and the `.dockerignore` (check that it
   does not drop `welcome.txt`).

The dojo checks that the started container has a volume mounted on `/data`, posts a message
and reads the list back.

Try the volume yourself: start the board on a named volume, post a message, remove the
container and start a new one on the same volume.

```
docker run -d --name board -p 8080:8080 -v board-data:/data guestbook
curl -X POST -d name=me -d text=hello localhost:8080/messages
docker rm -f board
docker run -d --name board -p 8080:8080 -v board-data:/data guestbook
curl localhost:8080/messages
```

> [!TIP]
> `VOLUME` in the Dockerfile makes Docker create an anonymous volume for every container that
> does not mount one itself. In production you still name the volume (`-v name:/data` or a
> Compose `volumes:` entry), so you can find it again.
