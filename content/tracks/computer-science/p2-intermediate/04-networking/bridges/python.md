---
from: python
status: draft
concept: "Networking in Python"
from_way: "Using requests.get in a loop without a session."
native_way: "socket is the BSD socket API; requests.Session and httpx.Client keep connections alive; asyncio streams use non-blocking sockets on an event loop."
sensei_tip: "Use a Session (or httpx.Client) with timeouts for every outbound call; a call without a timeout can hang forever on a lost packet."
---

| In Python | Underneath |
| --- | --- |
| `socket.create_connection` | DNS + TCP handshake |
| `requests.Session()` | keep-alive connection pool |
| `socket.getaddrinfo` | DNS resolution |
| `timeout=(3, 10)` | connect and read timeouts |
| `asyncio.open_connection` | non-blocking TCP |
