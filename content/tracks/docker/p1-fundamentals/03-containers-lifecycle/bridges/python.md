---
from: python
status: draft
concept: "Process lifecycle and graceful shutdown"
from_way: "Python raises KeyboardInterrupt on SIGINT but, by default, simply dies on SIGTERM without running finally blocks."
native_way: "In a container, SIGTERM is the normal stop request; register a handler with signal.signal(signal.SIGTERM, ...) or let your server (gunicorn, uvicorn) handle it."
sensei_tip: "As PID 1, a Python process without a SIGTERM handler ignores the signal and is killed after 10 seconds. Use the exec form and handle SIGTERM, or run with --init."
---

| Python | Container |
| --- | --- |
| `signal.signal(signal.SIGTERM, handler)` | how you react to `docker stop` |
| `sys.exit(0)` | a clean exit, exit code 0 |
| supervisord or systemd restarting a script | the restart policy |
| `subprocess` children | zombies unless PID 1 reaps them (`--init`) |
