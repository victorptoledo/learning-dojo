# Sidequest: Minimal HTTP server

Build `dojo::Server`, a small HTTP/1.1 server, in `server.hpp` and `server.cpp`, straight on
POSIX sockets (`socket`, `bind`, `listen`, `accept`, `poll`, `recv`, `send`): no libraries.

```cpp
dojo::Server server;
server.route("GET", "/hello", [](const dojo::Request&) {
    return dojo::Response{.status = 200, .headers = {}, .body = "hello"};
});
auto port = server.start();  // 127.0.0.1, any free port
```

It grows in five stages: answering one request, parsing requests properly, serving many at
once, defending against bad clients, and keeping connections alive with a clean shutdown.

- Every file descriptor is owned by an object that closes it in its destructor (RAII): no
  path, early return or exception may leak one.
- `start` returns at once; the server runs on threads of its own, and `stop` (also called by
  the destructor) ends them all. A thread blocked on a socket must notice a stop quickly:
  wait with `poll` and a short timeout instead of blocking forever.
- The contract tests talk to your server over real TCP connections on 127.0.0.1, inside the
  sandbox (which has no other network). From stage 3 on, everything runs under
  ThreadSanitizer; before that, under AddressSanitizer.

The dojo also checks the compiler warnings, clang-format and clang-tidy on your files. When
you are done, `[A]` copies a review prompt: ask an AI about your design.
