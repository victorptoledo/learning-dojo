# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole Node.js track. It has two parts, and
you need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards
   production, observability and performance. You need 7 right.
2. **The integration kata** (press **Enter**): a caching gateway in front of a slow, flaky
   product service. It brings the track together: an HTTP server with central error handling,
   `fetch` with a timeout, one upstream call shared by concurrent requests, a bounded cache,
   stale answers when the upstream fails, load shedding, a request id that follows the request
   through logs and the upstream, metrics, and a graceful shutdown.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| The Event Loop and libuv; Callbacks, Promises and async/await | What runs while a request waits for the upstream? How do several callers share one promise? What does an `AbortSignal` cancel? |
| HTTP Servers and Clients | Which status for a bad id, a missing product, a failed upstream, a timeout, too much load? Why exactly one response per request? |
| Node.js in Production | What does a load balancer read from `/ready`? In which order does a graceful shutdown happen, and what happens after the grace period? |
| AsyncLocalStorage, Logging and Observability | How does code far below the handler know the request id? What goes into an access log line, and what never does? |
| Graceful Server (kata) | Where must `requestContext.run` start? Why `connection: close` while stopping? |
| Performance, Profiling, Memory and Scaling | Why must a cache have a bound? What is a cache stampede? When do you refuse work instead of queueing it? |

> [!TIP]
> Every rule of the kata answers a production incident: an unbounded cache that eats the heap,
> a thousand requests that miss at the same second and knock the upstream over, a timeout that
> never comes, a queue that grows until latency is useless. Ask which one each line prevents.

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
