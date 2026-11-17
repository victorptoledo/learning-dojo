Listen, route and answer. `server.hpp` declares the interface; add the members and write them.

- `route(method, path, handler)` remembers a handler for an exact method and path.
- `start(port)` creates a TCP socket, binds it to `127.0.0.1:port` (`0` lets the system pick a
  free port; `getsockname` tells you which), listens, and starts one background thread that
  accepts connections. It returns the port, or a message saying why it failed: a port another
  server uses, or a server that is already running.
- For each connection: read until the blank line that ends the head (`\r\n\r\n`), take the
  method and the path from the request line (`GET /hello HTTP/1.1`), call the handler, write
  the response and close the connection. One connection at a time is fine for now.
- `stop()` makes the thread finish and closes the listening socket, so that connecting is
  refused afterwards; a second `stop()` does nothing, and the destructor stops too. The port
  can be used again right away (`SO_REUSEADDR`).

The response:

```text
HTTP/1.1 200 OK\r\n
Content-Type: text/plain\r\n        <- the handler's headers, in order
Content-Length: 5\r\n               <- always: the size of the body
Connection: close\r\n               <- always, for now
\r\n
hello
```

Reason phrases: 200 `OK`, 201 `Created`, 204 `No Content`, 400 `Bad Request`, 404 `Not Found`,
and `Unknown` for any other status for now (later stages add theirs). A request with no route
for its method and path gets 404 with the body `Not Found` (and `Content-Type: text/plain`); a
request line without three parts gets 400 `Bad Request`.

Write a `send` loop: one call may send only part of the data. Pass `MSG_NOSIGNAL`, or a client
that hangs up early kills the whole process with `SIGPIPE`.
