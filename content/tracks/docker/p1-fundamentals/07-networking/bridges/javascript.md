---
from: javascript
status: draft
concept: "Service addresses and ports"
from_way: "app.listen(3000) and a dev proxy to localhost:5000 connect everything on one machine."
native_way: "Each container has its own localhost. Call other services by name on a user-defined network, and bind servers to 0.0.0.0 (Node's default when no host is given)."
sensei_tip: "Vite and some dev servers bind to localhost only; pass --host 0.0.0.0 when they run in a container."
---

| JavaScript | Container networking |
| --- | --- |
| `app.listen(3000)` | listens on all interfaces: fine behind `-p 3000:3000` |
| `fetch("http://localhost:4000")` | the same container; use `http://api:4000` |
| `vite --host` | needed to be reachable from the host |
| CORS origins with `localhost` | the published host and port the browser uses |
