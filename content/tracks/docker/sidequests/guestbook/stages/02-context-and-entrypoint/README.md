The board works, but the build context carries files that have no business in an image, and
the container cannot take arguments. This stage added `notes/` and `admin.env` (with a real
password) to the folder.

1. Write a `.dockerignore` that keeps out of the build context: `.git`, `notes/`, the env
   files (`admin.env`, or `*.env`) and Markdown (`*.md`). Listing the `Dockerfile` and the
   `.dockerignore` themselves is a good habit too.
2. Copy `go.mod` on its own line **before** the `.go` files, so an edit to `main.go` does not
   invalidate the layers above it.
3. Make the binary the exec-form `ENTRYPOINT`, so `docker run guestbook -version` prints
   `guestbook 1.0.0` and `docker run guestbook` still starts the server.
4. Add the labels `org.opencontainers.image.title="guestbook"` and
   `org.opencontainers.image.description` (any short description).

The dojo now also runs your image as `docker run --rm IMAGE -version` and reads what it
prints. Everything from stage 1 must keep working: the title, `/health` and the logs.

> [!TIP]
> With `ENTRYPOINT ["guestbook"]` and no `CMD`, the arguments after the image name are
> appended to the entrypoint. With `CMD ["guestbook"]` they *replace* it, and
> `docker run guestbook -version` tries to run a program called `-version`.
