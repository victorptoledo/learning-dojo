---
from: python
status: draft
concept: "Service addresses and ports"
from_way: "flask run and many dev servers bind to 127.0.0.1 by default, and the database URL points at localhost."
native_way: "Inside a container, bind to 0.0.0.0 (flask run --host 0.0.0.0, uvicorn --host 0.0.0.0) and reach other services by their container or service name."
sensei_tip: "If curl works inside the container but the browser gets connection reset, check the bind address first: 127.0.0.1 is the most common cause."
---

| Python | Container networking |
| --- | --- |
| `app.run(host="127.0.0.1")` | unreachable through `-p`; use `0.0.0.0` |
| `DATABASE_URL=postgres://localhost/...` | `postgres://db/...` on a shared network |
| `socket.gethostname()` | the container ID or `--hostname` |
| `requests.get("http://localhost:9000")` | only reaches the same container |
