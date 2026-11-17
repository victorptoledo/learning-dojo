At-least-once delivery means a task can run twice, so producers and workers must be safe to
repeat.

- A third `enqueue` takes an **idempotency key**:

```java
public String enqueue(String type, String payload, String idempotencyKey)
```

  While a task with that key is not `DEAD`, enqueueing the same key again adds nothing and
  returns the id of the existing task (a retried HTTP request must not create a second
  payment). Once that task is dead, the key can be used again. Keys are compared exactly.

- Many worker threads share one queue. Under any mix of `enqueue`, `poll`, `ack` and `nack`
  from many threads: a task is never leased to two workers at once, every `ack` that returns
  `true` finishes a different delivery, and no task is lost. The tests run 8 workers over 2 000
  tasks.

Make it correct first (one lock around the state is fine), then ask the AI review how a real
queue avoids one global lock.
