# 🥋 Final Exam: Go in Production

This is the **final exam of the Go track**. It closes the **Mastery** phase, and passing it
means you can write, observe and operate a Go service. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the Mastery phase. You need 7 right.
2. **The integration project** (press **Enter**): a link shortener on `net/http`. Strict
   JSON input with a body limit, a deadline on every store call taken from the request's
   context, internal errors that reach the log but never the client, request IDs, one
   structured `slog` record per request, panic recovery, and a `Run` that drains readiness
   and then shuts down gracefully without cancelling the requests in flight.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| HTTP Services | Which pattern gives me a 405 for free? Which server timeouts must I set? What does `Shutdown` wait for? |
| To-do API | How do I answer JSON errors consistently? How do I recover a panic in middleware? |
| Structured Logging | Which attributes does every request record need? What must never be logged? |
| Profiling and Performance | How would I find out why this service is slow or uses too much memory, before changing code? |
| Go in Production | How does an orchestrator stop my container? Why is readiness different from liveness? |
| Context (P3) | Which context does a request get after the process was told to stop? |

> [!TIP]
> The hidden tests start real servers on `127.0.0.1` and send real requests while `Run`
> shuts down. Run the visible test with `-race` often: a counter, a flag or a log buffer
> touched by two requests at once shows up there first.
