# Greeter service: design notes

These notes are for people, not for the image. Nothing in `docs/` should reach the builder:
changing a sentence here must not trigger a rebuild of the service.

- `/health` answers `ok` while the process is up.
- `/hello` greets `GREETING_NAME`.
- Logs are JSON lines on stdout, one per request.
