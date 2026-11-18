# Sidequest: Lean URL shortener

The team's URL shortener ships as a 900 MB image that runs as root and carries the Go
toolchain and the sources. The program is fine (`main.go`, Go standard library only, links
kept in memory); the image is what you fix, in four stages:

1. a multi-stage build that ships only the binary;
2. a static binary on a minimal base: no shell, no package manager, at most 20 MB;
3. a numeric non-root user, and a build context that keeps secrets and notes out;
4. a health check that needs no shell, and a version stamped into the binary and the labels.

How it is checked:

- Your work is the `Dockerfile` and the `.dockerignore` at the top of this folder. You never
  need to edit `main.go`.
- The dojo builds the folder on your Docker engine with no network for `RUN` steps, starts the
  image once with no network and no capabilities, and checks it from the `dojo-docker` image:
  hadolint, then tests that read the image configuration and its file list, run it with
  arguments, and call `/health` and `/links`. Each stage keeps every earlier check.
- The build cache stays between checks, so a re-check rebuilds only what changed.

When you are done, `[A]` copies a review prompt: ask an AI about your Dockerfile.
