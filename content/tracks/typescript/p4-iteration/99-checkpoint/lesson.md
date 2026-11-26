# Checkpoint 4: Iteration

This checkpoint closes the **Iteration** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a streaming log report. Chunks become lines
   (with characters split between chunks), lines become entries, entries become time windows,
   every stage an async generator that reads only what it needs. Then a `mapConcurrent` that
   keeps source order, limits the calls in flight, and aborts and closes everything when the
   run stops.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Iterators, Generators and Lazy Sequences | What is the difference between an iterable and an iterator? When does a generator's body run? What does `return()` do to a paused generator? |
| Lazy Sequences (kata) | How do you avoid pulling one item too many? How does `zip` close the sources it did not finish? |
| Async Iteration, Streams and Backpressure | Why are chunks not lines? What gives `for await` its backpressure, and what breaks it? What actually stops a pending `fetch`? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
