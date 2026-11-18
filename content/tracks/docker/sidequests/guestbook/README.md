# Sidequest: Guestbook

A club wants a message board for its door screen: visitors leave a name and a line of text,
and the screen lists them. The program is written (`main.go`, Go standard library only); your
job is the image that runs it, in three stages:

1. a `Dockerfile` that builds and starts the board;
2. a clean build context and an entrypoint that takes arguments;
3. messages that survive the container, on a volume, with a welcome message on every new
   board.

How it is checked:

- Your work is the `Dockerfile` (and, from stage 2, the `.dockerignore`) at the top of this
  folder. You never need to edit `main.go`.
- The dojo builds the folder on your Docker engine with **no network** for `RUN` steps (only
  the `FROM` images are pulled), starts the image once with no network and no capabilities,
  and checks it from the `dojo-docker` image: hadolint on the `Dockerfile`, then tests that
  read the image configuration, list its files, run it with arguments and call its HTTP
  endpoints. The image and the container are removed afterwards.
- hadolint findings at warning level fail the check; the message names the rule (`DL3025`
  and so on), which you can look up.

Build and run it yourself with `docker build -t guestbook .` and
`docker run --rm -p 8080:8080 guestbook`.

When you are done, `[A]` copies a review prompt: ask an AI about your Dockerfile.
