---
from: python
status: draft
concept: "From WSGI/ASGI to node:http"
from_way: "An ASGI or WSGI server (uvicorn, gunicorn) calls your app; Flask, FastAPI or Django route, parse and validate; httpx or requests call other services."
native_way: "node:http is server and protocol layer at once and calls your listener; Express or Fastify route and parse; fetch calls other services."
sensei_tip: "There is no separate application server: the Node.js process is the server. FastAPI's schema validation corresponds to Fastify's JSON Schema routes."
---

| Python | Node.js |
| --- | --- |
| uvicorn + ASGI app | `createServer(listener)` |
| `@app.get("/users/{id}")` | `app.get("/users/:id", h)` (Fastify, Express) |
| `await request.json()` | Read `req`, then `JSON.parse` |
| `HTTPException(404)` | `throw new HttpError(404, ...)` + one handler |
| `httpx.get(url, timeout=3)` | `fetch(url, { signal: AbortSignal.timeout(3000) })` |
