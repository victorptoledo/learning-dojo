# Sidequest: Release image

The warehouse's inventory API (`cmd/inventory` and `internal/stock`, Go standard library
only) is ready; its image is not. Take it through the team's release checklist, in five
stages:

1. a fixed non-root user that can write its data folder and nothing else;
2. builds that send only what they compile and keep the compiler cache;
3. a `scratch` runtime that still has CA certificates, time zones and a `/tmp`;
4. the server as PID 1, a health check, a version stamped at build time and the data on a
   volume;
5. the release audit: bases pinned by digest, OCI labels, and no secret in the image, its
   history or its build context.

How it is checked:

- Your work is the `Dockerfile` and the `.dockerignore` at the top of this folder. You never
  need to edit the Go code.
- The dojo builds the folder on your Docker engine with no network for `RUN` steps, runs it
  as `docker run --rm IMAGE -preflight` and `-version`, starts it once with no network and no
  capabilities, and checks it from the `dojo-docker` image: hadolint, the image
  configuration, its file list and history, and the API itself.
- `inventory -preflight` is the program's own report of what it can see and do: its PID, its
  user, whether it can write its binary, its data folder and `/tmp`, and whether CA
  certificates and time zones load. It is how the checks look inside an image with no shell.

When you are done, `[A]` copies a review prompt: ask an AI about your Dockerfile.
