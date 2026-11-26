# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a basket quote. A `Money` type built from
   special methods, and an async `quote()` that asks several price sources in batches, with a
   concurrency limit, a timeout per call and partial failure, and stays fast on 20,000 items.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| asyncio, Threads, Processes and the GIL | Which work do threads speed up on the standard build, and which do they not? What does a `TaskGroup` do when one task fails, and when the task around it is cancelled? |
| Async Fetch (kata) | Where does the `Semaphore` go so that waiting for a slot is not timed? Why must `CancelledError` never be swallowed? |
| Protocols, ABCs and Special Methods | When do you return `NotImplemented` instead of raising? Why do `__eq__` and `__hash__` have to agree? |
| Performance and Profiling | What do you run before you change anything? Which loops hide an O(n²)? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
