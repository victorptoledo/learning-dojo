---
from: python
status: draft
concept: "Client-server communication"
from_way: "requests and FastAPI hiding HTTP behind functions and decorators."
native_way: "HTTP semantics in your hands: methods, status codes, caching headers and conditional requests."
sensei_tip: "FastAPI lets you return any status; choose them for their meaning. Response headers such as ETag and Cache-Control are set with a Response parameter or middleware."
---

| Python | In practice |
| --- | --- |
| @app.post returning 200 | 201 Created with Location |
| requests.get(url) | a GET that is safe and idempotent |
| Response(headers={'ETag': ...}) | conditional GET with 304 |
| httpx with http2=True | HTTP/2 multiplexing |
