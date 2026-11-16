---
from: python
status: draft
concept: "Serving in production"
from_way: "gunicorn or uvicorn workers supervised by the server process, graceful timeouts in the server's settings, and lifespan events for startup and shutdown."
native_way: "The Node.js process is the server: you handle SIGTERM yourself, drain the HTTP server, close resources, and rely on the platform to restart and scale."
sensei_tip: "What gunicorn's --graceful-timeout and ASGI lifespan shutdown give a Python app, a Node.js app writes in its own entry point. There is no master process unless you add one."
---

| Python | Node.js |
| --- | --- |
| `gunicorn --graceful-timeout 20` | Your shutdown timeout |
| ASGI lifespan `shutdown` | `process.on("SIGTERM", shutdown)` |
| gunicorn master restarts workers | The platform restarts the process |
| `pip install --no-dev` style image | `npm ci --omit=dev` |
| `logging` to stdout | pino to stdout |
