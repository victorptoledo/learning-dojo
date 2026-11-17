Failures now cost time, and too many of them end the task's life.

- After a `nack` of attempt `n`, the task is ready again after
  `min(maxDelay, baseDelay × 2^(n-1))`: with a base of 1 s, 1 s after the first failure, 2 s
  after the second, then 4 s, 8 s... never more than `maxDelay`. Until then `poll` skips it
  (and its status is `READY`).
- A delivery that fails on attempt `maxAttempts`, by a `nack` or by its lease running out,
  makes the task `DEAD` instead. It is never delivered again.
- A lease running out is a failure too, with the error `lease expired`. The task is ready
  again as soon as the lease ends (no backoff: nobody said it failed).

New in the starter, `DeadLetter(Task task, String lastError)`, and two methods:

```java
public List<DeadLetter> deadLetters()   // in the order the tasks died; task = the last delivery
public boolean requeue(String id)       // a DEAD task becomes READY with its attempts reset
```

`requeue` of a task that is not dead returns `false`. After a requeue the next delivery is
attempt 1 again.
