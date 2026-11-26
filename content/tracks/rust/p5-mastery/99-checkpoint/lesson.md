# 🥋 Final Exam: Mastery

This is the **final exam** of the Rust track. It closes the **Mastery** phase and, with it, the
whole track. You need both parts:

1. **The quiz** (press **Q**): eight questions over async, futures and production Rust. You need
   7 right.
2. **The integration kata** (press **Enter**): a health probe, the kind of tool an operator runs
   during an incident. It needs a small runtime that you write yourself (sleep, join, timeout and
   a parking `block_on`), configuration from the environment with typed errors and a cause chain,
   structured log lines with levels, and exit codes.

Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| async/await and Tokio | Why does `.await` in sequence take the sum of the waits, and `join!` only the longest? What does dropping a future do? Why never block inside async code? |
| Futures by Hand | Who calls `wake`, with which waker? Why park and not spin? When must a future no longer be polled? Why `Pin<Box<F>>`? |
| Production Rust | Which errors does a caller match on, and which does a human read? How do I keep the cause of an error? What must a log line contain to be searchable? |
| Checkpoint 3 | How did I share a callback between threads with `Arc<dyn Fn ...>`? |

> [!TIP]
> Time is part of the spec: several tests measure how long things take. Run the visible tests
> often, and when a test about timing fails, look for the place where your code **waits** instead
> of returning `Pending`.
