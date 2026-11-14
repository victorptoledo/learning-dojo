# Resilient Client

## 1. `RetryPolicy(maxAttempts, initialDelay, multiplier, maxDelay)`

`delayBefore(attempt)` is the wait before attempt number `attempt` (attempt 1 never waits):
`initialDelay * multiplier^(attempt - 2)`, capped at `maxDelay`. With 100 ms, ×2, max 500 ms:
attempt 2 → 100 ms, 3 → 200 ms, 4 → 400 ms, 5 and later → 500 ms.

## 2. `CircuitBreaker(failureThreshold, openDuration, clock)`

```text
CLOSED ──(failureThreshold consecutive failures)──▶ OPEN ──(openDuration passes)──▶ HALF_OPEN
   ▲                                                   ▲                                │
   └──────────────(trial call succeeds)────────────────┼────────(trial call fails)──────┘
```

- `call(action)`: in **CLOSED** run it; a success resets the failure count, a failure counts one
  (any `RuntimeException`) and is rethrown; reaching the threshold **opens** the circuit.
- In **OPEN**, throw `CircuitOpenException` **without calling** the action.
- Once `openDuration` has passed since it opened, the state is **HALF_OPEN**: the next call is a
  trial; success → CLOSED (count reset), failure → OPEN again (the timer restarts).
- `state()` reports the current state, taking the clock into account.

## 3. `ResilientClient(retry, breaker, sleeper)`

`execute(call)` runs the call **through the breaker**, up to `maxAttempts` times:

- a `TransientException` is retried after `sleeper.sleep(retry.delayBefore(nextAttempt))`;
- any other exception (a bug, a `400`), **including `CircuitOpenException`**, is rethrown at once,
  without retrying: retrying a request that is wrong, or a circuit that is open, only adds load;
- after the last attempt, the last `TransientException` is thrown.

> Tip: keep the breaker's state in fields guarded by `synchronized`; remember when it opened
> (`clock.instant()`) and compare with `openedAt.plus(openDuration)`. Real retry libraries also add
> **jitter** (a random part) to the delays so many clients do not retry in lockstep.
