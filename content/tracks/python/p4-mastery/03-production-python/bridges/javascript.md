---
from: javascript
status: draft
concept: "Running Python services like Node services"
from_way: "pino or winston JSON logs, dotenv and process.env, npm ci in a Dockerfile, PM2 or cluster mode for several processes, npm audit and Dependabot, and SIGTERM handlers."
native_way: "logging with structlog or a JSON formatter, pydantic-settings, uv sync --locked in a Dockerfile, several worker processes, pip-audit and Dependabot, and SIGTERM handled by the server and your workers."
sensei_tip: "Python has the same one-core-per-process limit as Node, so the operational playbook is the same: several processes per host or more replicas, and a load balancer in front."
---

| Node.js | Python |
| --- | --- |
| `pino` / `winston` | `logging` + `structlog` |
| `dotenv` + `process.env` | `pydantic_settings.BaseSettings` (reads `.env` too) |
| `npm ci --omit=dev` | `uv sync --locked --no-dev` |
| PM2 / cluster mode | Uvicorn `--workers`, Gunicorn, or replicas |
| `npm audit` | `pip-audit` |
| `process.on("SIGTERM", ...)` | `signal.signal(SIGTERM, ...)` / `loop.add_signal_handler` |
| `eval` / `child_process.exec` with input | `eval` / `subprocess` with `shell=True`: never |
| `node:22-slim` | `python:3.15-slim` |
