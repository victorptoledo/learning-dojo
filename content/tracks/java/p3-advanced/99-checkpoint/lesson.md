# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a freight quoter that asks every carrier at
   once on virtual threads, within one deadline and each carrier's limit, passes the order id
   in a scoped value, counts calls without races and picks the best offer with a strategy
   written as a lambda.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Virtual Threads (Project Loom) | Why is blocking cheap again? Why not pool virtual threads, and what do I limit instead? |
| Executors, Futures and Thread Safety | How do I wait with a limit and cancel? Which tool keeps a shared counter correct? Where is a `ScopedValue` visible? |
| Parallel Fetch (kata) | Why submit everything before waiting? How does one deadline differ from one timeout per call? |
| Design Patterns in Modern Java | When is a strategy just a lambda? What does a `switch` over a sealed type give me over a visitor? |

> [!TIP]
> Concurrency bugs hide in the paths you do not test: the permit that is not given back after
> an exception, the call that keeps running after its timeout. The grading tests take those
> paths on purpose.
