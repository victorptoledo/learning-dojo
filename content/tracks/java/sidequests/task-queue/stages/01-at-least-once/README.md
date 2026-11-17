```java
public TaskQueue(Clock clock, QueueConfig config)
public String enqueue(String type, String payload)   // a new id; the task is READY
public Optional<Task> poll()                          // leases the next ready task
public boolean ack(Task delivery)                     // finishes it: DONE
public boolean nack(Task delivery, String error)      // it failed: it will come back
public Status status(String id)                       // READY, LEASED, DONE (DEAD in stage 2)
```

- `poll` hands out the task that has been ready the longest (the first enqueued, at first),
  as a `Task` whose `attempt` is 1 on its first delivery, 2 on the second... The task is then
  `LEASED` for `config.lease()`: no other `poll` returns it.
- `ack` within the lease makes the task `DONE` and returns `true`.
- When a lease runs out without an ack, the task is ready again from the moment the lease
  ended, and its next delivery has the next attempt number.
- An ack or nack of a delivery that is no longer current (its lease ran out, or it was
  already acked or nacked) changes nothing and returns `false`. This is what makes
  at-least-once safe: a slow worker cannot finish a task another worker now holds.
- `nack` gives the task back at once: it is `READY` again (stage 2 adds a delay).
- `status` of an id the queue never gave out throws `NoSuchElementException`.

`poll` returns `Optional.empty()` when nothing is ready.
