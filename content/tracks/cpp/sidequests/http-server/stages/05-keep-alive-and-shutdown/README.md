Keep connections alive, and shut down cleanly. Add:

```cpp
// In ServerConfig, after read_timeout:
std::chrono::milliseconds idle_timeout{5000};  // between requests on one connection

// Member of Server: how many responses the server has sent (errors too).
std::size_t requests_served() const;
```

- **Keep-alive.** An HTTP/1.1 connection stays open after a response, and the next request may
  follow on it, even before the response (pipelining: answer in order). The response says
  `Connection: keep-alive`. It closes after the response when the request says
  `Connection: close` (any case), when the request is HTTP/1.0, or after an error response;
  then the response says `Connection: close`.
- **Idle timeout.** Between two requests, the connection waits at most `idle_timeout` for the
  first byte of the next one, then closes quietly (no 408: nothing was asked). Once a request
  has started, `read_timeout` applies as before. The first request of a connection keeps the
  stage 4 rule.
- **Graceful shutdown.** `stop()` refuses new connections at once, lets every request being
  handled finish (its response says `Connection: close`), closes idle connections without
  waiting for their timeout, and returns when every thread is done.
- **`requests_served()`** counts every response sent, from any thread: a `std::atomic`.

A kept-alive connection holds its worker while it waits. That is the price of this design;
servers that keep thousands of idle connections use one thread with `epoll` instead (ask the
review prompt about it).
