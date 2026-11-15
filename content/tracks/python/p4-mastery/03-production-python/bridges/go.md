---
from: go
status: draft
concept: "From a static binary to an interpreter plus environment"
from_way: "log/slog with a JSON handler, configuration from flags and environment variables, one static binary in a scratch or distroless image, one process using every core, signal.NotifyContext for shutdown, and govulncheck."
native_way: "logging (or structlog) with JSON output, pydantic-settings, an image with the interpreter and a locked .venv, several worker processes, SIGTERM handling, and pip-audit."
sensei_tip: "You cannot ship Python as one static binary, so the image is the binary: lock it with uv.lock, keep it slim, and rebuild it often for base-image patches."
---

| Go | Python |
| --- | --- |
| `slog.New(slog.NewJSONHandler(os.Stdout, nil))` | `logging` with a JSON formatter, or `structlog` |
| `slog.Info("charged", "order", id)` | `log.info("charged %s", order_id)` / `structlog` key-values |
| `envconfig` / `flag` | `pydantic_settings.BaseSettings` |
| a static binary in a scratch image | `python:3.15-slim` + `.venv` |
| one process, every core | several worker processes |
| `signal.NotifyContext(ctx, syscall.SIGTERM)` | `loop.add_signal_handler(signal.SIGTERM, ...)` |
| `govulncheck` | `pip-audit` |
