# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a monitor over a stream of request events that
   answers "how many, how many users, how many from this user, the worst and the average
   latency in the last N milliseconds" in O(1) each. It combines a queue, a hash map and a
   monotonic deque.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Arrays, Strings and Their Costs | Why is indexing O(1)? What does `s += x` in a loop cost? What is a code point? |
| Hash Tables Inside | What do `hashCode` and `equals` each decide? What does a resize cost, and when? |
| Hash Map (kata) | Why spread the hash bits? Why a power-of-two bucket count? |
| Linked Lists, Stacks, Queues and Deques | Which operations are O(1) on a linked list, and which are not? Why `ArrayDeque`? |
| Two Pointers, Sliding Windows and Prefix Sums | What invariant lets each index move only forward? When do negative values break a window? |
| Sequence Toolkit (kata) | What does a monotonic stack keep, and why is it amortised O(n)? |

> [!TIP]
> The kata's key idea is the **monotonic deque** for the window maximum: an event with a smaller
> latency that arrived before a larger one can never be the maximum again, so drop it. Each
> event enters and leaves the deque at most once, which is why the whole stream costs O(n).
