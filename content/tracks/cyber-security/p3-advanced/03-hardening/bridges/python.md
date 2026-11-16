---
from: python
status: draft
concept: "Run only what is needed, with only the rights it needs"
from_way: "gunicorn as root on 0.0.0.0, the debug server in production, and pip install of whatever is newest."
native_way: "An unprivileged user, gunicorn or uvicorn behind a reverse proxy, a slim image, locked dependencies and automatic OS updates."
sensei_tip: "Flask's and Django's development servers are not hardened for production; run a WSGI or ASGI server as a dedicated user and let a reverse proxy own ports 80 and 443."
---

| Python | Hardened deployment |
| --- | --- |
| flask run --host 0.0.0.0 | gunicorn behind nginx |
| root in the container | USER app in the Dockerfile |
| python:3 full image | a slim image, rebuilt often |
| pip install without pins | a lockfile |
| debug toolbars installed | dev-only dependency groups |
