# Event Window

A monitoring agent receives a stream of request events (time, user, latency) and must answer,
at any moment, questions about the **last `windowMillis` milliseconds**: how many requests,
how many distinct users, how many from one user, the worst latency and the average latency.
The stream is long (millions of events) and the window can hold hundreds of thousands of
them, so every question must be answered in **O(1)** (amortised), never by scanning the
window.

```java
var window = new EventWindow(1_000);       // the last second
window.record(100, "ada", 30);
window.record(400, "bob", 90);
window.record(900, "ada", 50);
window.count();          // 3
window.distinctUsers();  // 2
window.maxLatency();     // OptionalInt[90]
window.advanceTo(1_400); // the events at 100 and 400 leave: 400 <= 1400 - 1000
window.count();          // 1
window.maxLatency();     // OptionalInt[50]
```

## The window

- The clock starts at `0`. `record(timestamp, ...)` and `advanceTo(now)` move it forward; a time
  earlier than the clock throws `IllegalArgumentException` (equal is fine).
- An event is **in the window** while `timestamp > now - windowMillis`.
- `windowMillis < 1`, a `null` or blank user, or a negative latency throw
  `IllegalArgumentException`.

## Questions

| Method | Returns |
| --- | --- |
| `now()` | the clock |
| `count()` | events in the window |
| `distinctUsers()` | distinct users with at least one event in the window |
| `countFor(user)` | that user's events in the window (0 for an unknown user) |
| `maxLatency()` | the largest latency in the window, or `OptionalInt.empty()` |
| `averageLatency()` | the mean latency in the window, or `OptionalDouble.empty()` |

## The structures you need

This is the integration kata of the Fundamentals phase: each question maps to one structure
from it.

- A **queue** (`ArrayDeque`) of the events, oldest first: the oldest is the next to leave.
- A **hash map** from user to count, updated on arrival and departure; remove a user whose count
  reaches 0, so its size is the number of distinct users.
- A running **sum** of the latencies for the average.
- A **monotonic deque** for the maximum: it keeps only the events that can still become the
  maximum, in arrival order with decreasing latencies. A new event removes every event at the
  back with a smaller latency (they leave earlier and are smaller, so they can never be the
  maximum again). The front is the maximum; when the front event leaves the window, drop it.

The hidden tests compare every answer with a slow reference over random streams, and time a
stream of a million events with a window of 200,000.
