Defend the server against clients that send too much or too little. `ServerConfig` grows:

```cpp
struct ServerConfig {
    unsigned workers = 4;
    std::size_t max_header_bytes = 8192;           // the request line and the headers
    std::size_t max_body_bytes = 1 << 20;          // the body
    std::chrono::milliseconds read_timeout{5000};  // to receive one whole request
};
```

| Situation | Response |
| --- | --- |
| more than `max_header_bytes` arrive without the blank line that ends the head | **431** `Request Header Fields Too Large` |
| `Content-Length` above `max_body_bytes` (or too big for a number) | **413** `Content Too Large`, sent **before** reading the body |
| the whole request (head and body) has not arrived `read_timeout` after the worker took the connection, including a client that sends nothing | **408** `Request Timeout` |

All three close the connection. A client that stalls holds one worker until its timeout, and
the other workers go on serving: wait with `poll` against a deadline (`steady_clock`, never
the wall clock) instead of a blocking `recv`.

Closing a socket that still has unread data makes the kernel reset the connection, and the
client may never see your error response. After sending one, shut down your side for writing
(`shutdown(fd, SHUT_WR)`) and read and drop what still arrives for a moment before closing.
