---
from: python
status: draft
concept: "Logs and diagnostics"
from_way: "logging.FileHandler with rotation, and pdb or py-spy on the machine where the script runs."
native_way: "Use a StreamHandler to stdout (JSON formatted), set PYTHONUNBUFFERED=1, and debug with docker logs, docker exec and py-spy in a container that shares the PID namespace."
sensei_tip: "If docker logs stays empty while the app clearly runs, stdout is buffered: PYTHONUNBUFFERED=1 or python -u fixes it."
---

| Python | Containers |
| --- | --- |
| `RotatingFileHandler` | stdout plus the log driver's rotation |
| `print()` buffered when piped | `PYTHONUNBUFFERED=1` |
| `py-spy dump --pid` | `docker run --pid container:api` with py-spy |
| `breakpoint()` | attach with `docker attach` or debug locally with a bind mount |
