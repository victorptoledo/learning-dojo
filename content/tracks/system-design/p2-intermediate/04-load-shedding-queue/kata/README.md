# Load-Shedding Queue

`LoadSheddingQueue` sits between a server's request handlers and its workers. It never grows
past its capacity, it keeps room for **critical** work, and it never hands a worker a job whose
client has already given up.

## `LoadSheddingQueue(int capacity, int sheddableLimit, Duration maxWait, Clock clock)`

- `capacity` at least 1, `sheddableLimit` between 0 and `capacity`, `maxWait` positive;
  otherwise `IllegalArgumentException`.

## `offer(job, priority)` → `true` if queued

| Priority | Accepted when | Otherwise |
| --- | --- | --- |
| `SHEDDABLE` | fewer than `sheddableLimit` jobs are queued (of any priority) | rejected |
| `CRITICAL` | fewer than `capacity` jobs are queued | if a sheddable job is queued, the **newest** sheddable job is **shed** (removed) and the critical job is queued; else rejected |

## `poll()` → `Optional<String>`

- **Critical** jobs first, oldest first; then **sheddable** jobs, oldest first.
- A job is **expired** once `maxWait` has passed since it was offered (at exactly `maxWait` it
  is expired). Expired jobs are dropped, never returned.

## Expiry everywhere

Expired jobs are dropped (and counted) whenever the queue is used: at the start of `offer`,
`poll` and `size`. So they free room for new work, and `size()` counts only live jobs.

## `stats()` → `QueueStats(accepted, rejected, shed, expired, served)`

`accepted` and `rejected` count offers; `shed` counts sheddable jobs pushed out by critical
ones; `expired` counts jobs dropped for waiting too long; `served` counts jobs returned by
`poll`. Every method is **thread-safe**: 1,000 concurrent offers into a queue of 100 accept
exactly 100.

> Tip: two `ArrayDeque<Job>`, one per priority, with `record Job(String id, Instant offeredAt)`.
> The clock never goes back, so the oldest job of each deque is at its head: drop expired jobs
> with `while (!q.isEmpty() && expired(q.peekFirst()))`. The newest sheddable job is
> `pollLast()`.
