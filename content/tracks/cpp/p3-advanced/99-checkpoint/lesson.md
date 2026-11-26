# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a telemetry hub. Events are a `std::variant`
   (the set is closed), rules are a class hierarchy (anyone can add one), listeners are
   move-only callables, alerts come out of a `std::generator` one at a time, and
   `publish_all` feeds the hub from several `std::jthread`s at once. The tests run under
   **ThreadSanitizer**, so a single unprotected read fails them.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Ranges, Views, Projections and Generators | Is this view lazy? Who keeps the data it reads alive? What does the generator's frame own? |
| Log Pipeline (kata) | Which pipeline step filters, which transforms, and where do I collect with `ranges::to`? |
| Threads, Mutexes, Atomics and the Memory Model | Which data is shared? Which lock protects it, and am I holding it while calling code I do not control? |
| Task Queue (kata) | Who joins the threads, and when? |
| Polymorphism: Virtual Functions, std::variant and Type Erasure | Is the set of cases closed (variant) or open (virtual)? Does a copy slice it? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
