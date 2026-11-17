Serve many clients at once. Add to `server.hpp`, and give `Server` a constructor that takes it:

```cpp
struct ServerConfig {
    unsigned workers = 4;  // requests handled at the same time
};

explicit Server(ServerConfig config = {});
```

- The accepting thread only accepts: it hands each connection to a queue, and `workers`
  threads take connections from the queue and serve them. A slow handler then holds one
  worker, not the whole server.
- At most `workers` handlers run at the same time; connections that arrive meanwhile wait in
  the queue and are served when a worker is free.
- The queue is shared by several threads: a `std::mutex` protects it and a
  `std::condition_variable` wakes a worker when a connection arrives (and every worker on
  stop). The routes are only read once the server runs, so they need no lock.
- `stop()` stops accepting, closes the listening socket at once, lets each worker finish the
  request it is handling (its client gets the whole response), and joins every thread.
  `std::jthread` joins in its destructor; `notify_all` after setting the stop flag under the
  mutex, or a worker can miss the wake-up and wait forever.

Handlers are now called from several threads at once: that is the handler's business, but
everything of yours they touch must be safe. From this stage on, all the tests run under
ThreadSanitizer, which fails the check on any data race.
