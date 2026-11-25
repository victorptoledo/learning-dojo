# Checkpoint 3: A Hardened Statements API

A bank's statements API is going from one instance to many, behind a load balancer. Before it
scales, it has to be safe, it has to survive its slowest dependency, and the team has to know when
it is hurting users. The entities, the views, the error handling, the rates service
(`FakeRates`), the circuit breaker and the retry policy from the Resilient Client kata
(`CircuitBreaker`, `RetryPolicy`, `Sleeper`) and the beans that wire them (`ResilienceConfig`) are
written. You write `AccountController`, `AccountService`, `ResilientRates`, `SloTracker` and
`SloFilter`, and you fix `EntrySearch`.

The gateway authenticates the caller and passes the user in the `X-User` header. Without it, the
API answers `401` (`ApiErrors` already does this when you read the header with
`@RequestHeader("X-User")`).

## 1. API security

| Request | Answer |
| --- | --- |
| `GET /accounts/{id}` | `200`, an `AccountView` |
| `PATCH /accounts/{id}` `{"nickname": "..."}` | `200`, the updated `AccountView` |
| `GET /accounts/{id}/entries?q=&sort=-bookedOn&size=20` | `200`, a list of `EntryView` |
| `GET /accounts/{id}/balance?in=EUR` | `200`, a `BalanceView` (part 2) |

- **Object-level authorization (BOLA).** Every endpoint checks that the account belongs to the
  caller. Someone else's account gets `StatementsException.accountNotFound()`, the **same 404** as
  an id that does not exist, so the answer reveals nothing. Check before doing any other work.
- **Mass assignment.** `PATCH` binds the `AccountUpdate` record (validated: not blank, at most 40
  characters, else `400`), never the `Account` entity. Fields such as `owner`, `balanceCents` or
  `overdraftLimitCents` in the body change nothing. Save the change in a transaction.
- **Excessive data.** Return `AccountView`, which has no `owner` and no overdraft limit.
- **Injection.** `EntrySearch` builds its SQL by concatenation. Keep `JdbcTemplate`, and:
  - `sort` is one of `bookedOn`, `-bookedOn` (the default), `amount` and `-amount`, mapped to a
    fixed `order by` (ties by id). Anything else is `StatementsException.badRequest(...)`. A column
    name cannot be a bound parameter, so the allow-list is the only defence.
  - `q` (optional) finds the entries whose description contains it, ignoring case. Bind it as a
    parameter. A `%` or `_` in `q` is a character to find, not a wildcard: escape it and add
    `escape '\'` to the `like`.
- **Resource consumption.** `size` is from 1 to 100, else `400`.

## 2. Resilience: `ResilientRates.rate(from, to)`

`GET /accounts/{id}/balance?in=EUR` converts the balance with a rate from the rates service:
`balanceCents × rate`, rounded half-even to whole cents. The same currency needs no rate (`1`).

- Call the service **through the breaker**, up to `retry.maxAttempts()` times, and sleep
  `retry.delayBefore(attempt)` (with the injected `Sleeper`) before every attempt after the first.
- Only a `TransientException` is retried. An `UnknownCurrencyException` goes straight out (`ApiErrors`
  makes it a `400`). A `CircuitOpenException` ends the attempts at once: retrying an open circuit
  only adds load.
- After a success, save the rate as a `LastRate` with the key `"USD:EUR"`. With several instances
  behind a load balancer, a field in one of them would help only the requests that reach it; the
  database is shared by all of them.
- When the service cannot answer (retries used up, or the circuit open), use the `LastRate` and
  mark the quote `stale`. With no last rate, throw `StatementsException.ratesUnavailable(...)`
  with the breaker's open duration: `503` with `Retry-After: 30`.

## 3. SRE: `SloFilter` and `SloTracker`

The SLI is the share of good requests to `/accounts...`; the SLO is 99% over a sliding hour.

- `SloFilter` records every request whose path starts with `/accounts`: **good** if the status is
  below 500, **bad** if it is a 5xx or an exception escaped. A `4xx` is the client's mistake and
  does not burn the budget. `/slo` itself is not counted.
- `SloTracker.report()` looks only at the requests recorded in the last `WINDOW` (strictly after
  now minus one hour, by the injected `Clock`):
  - `availability = good / total`;
  - `burnRate = (bad / total) / (1 − OBJECTIVE)`: 1 means the budget lasts exactly the window;
  - `budgetRemaining = 1 − burnRate` (negative once it is overspent);
  - `paging = burnRate ≥ PAGE_BURN_RATE`.

  With no requests in the window, the report is `0, 0, 1.0, 0.0, 1.0, false`.
- `GET /slo` (written) returns the report.

> Tip: a private `owned(user, id)` that does `findById(id).filter(owner equals user)` serves every
> endpoint. The `Map` from `sort` to `order by` is the allow-list. In `SloFilter`, override
> `shouldNotFilter` and record in a `finally`. `SloTracker` can keep `(instant, good)` samples in an
> `ArrayDeque` and drop the old ones when it reports; make both methods `synchronized`.
