# Checkpoint 5: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions, weighted towards the JVM, native images and
   production Java, with the concurrency of the advanced phase underneath. You need 7 right.
2. **The integration kata** (press **Enter**): an order ingest service that works on a laptop
   and fails a production readiness review six times: configuration that fails late and leaks
   the password, eager and leaky logs, a static cache that only grows, a parser over its
   allocation budget, a shutdown that drops orders, and enrichers loaded by reflection that a
   native image cannot see.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| The JVM Under Load: Memory, GC and Profiling | What keeps an object alive? Where do allocations come from, and how do I measure them instead of guessing? |
| GraalVM Native Image: Millisecond Startup | What does the closed world miss? How do I replace reflection with code the build can see? |
| Production Java: Logs, Config and Packaging | When should a bad setting be noticed? What must never reach a log? How does a service stop without losing work? |
| Executors, Futures and Thread Safety | What is the difference between `shutdown` and `shutdownNow`? What do I do with `InterruptedException`? |

> [!TIP]
> Fix one incident at a time and run the tests after each. The tests are grouped by incident,
> so a red test tells you which review finding is still open.
