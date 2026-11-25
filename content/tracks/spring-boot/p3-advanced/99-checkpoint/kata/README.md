# Checkpoint 3: The Exchange-Rate Desk

A desk publishes how many units of each currency one euro buys. The application already works:
`RateController`, the `Rate` entity, the records, the users and the error handling are written,
and `RateService` reads and writes rates. What is missing is everything around it. You write
`SecurityConfig`, `RateChangeListener` and `RatesHealthIndicator`, and you change `RateService`
(and `DojoApplication`, or any configuration class you like).

Two users exist (see `UsersConfig`): `viewer` / `viewer-pass` with role `USER`, and
`trader` / `trader-pass` with roles `USER` and `TRADER`.

## Endpoints

| Request | Who | Answer |
| --- | --- | --- |
| `GET /rates/{code}` | anyone | `200` `{"code": "USD", "perEuro": 1.08, "updatedAt": "..."}`; unknown → `404` |
| `PUT /rates/{code}` `{"perEuro": 1.1}` (validated) | a trader | `200`, the rate (created if new) |
| `GET /actuator/health` | anyone | `200` when everything is `UP`, `503` when something is `DOWN` |

## Rules

1. **Security.** A `SecurityFilterChain` for a stateless JSON API: `GET /rates/**` and
   `/actuator/health/**` are public, everything else needs a login with **HTTP Basic** (no or
   wrong credentials: `401`); no sessions, CSRF off.
2. **Only traders change a rate,** and the rule sits on `RateService.update` itself
   (`@PreAuthorize`), so no other caller can skip it. A logged-in viewer gets `403`.
3. **Reads are cached** in a cache named `rates`: the second read of the same rate runs **no
   SQL**. A change clears that rate from the cache, so the next read shows the new value. A
   missing rate is not cached.
4. **Every committed change is audited and counted.** `update` publishes a `RateChanged` (with
   the previous value, or `null` for a new currency, and the name of the logged-in user).
   `RateChangeListener` records `change.describe()` in the `AuditTrail` and increments the
   counter `desk.rate.updates` with the tag `currency`, **only after the transaction commits**:
   a change that rolls back leaves no audit line and no count.
5. **A health check named `rates`.** `RatesHealthIndicator` is `DOWN` with the detail `stale`
   (the list of codes) when any rate was set longer ago than `desk.max-age` (one hour in
   `application.properties`), and `UP` otherwise. Read the time from the injected `Clock`: the
   tests move it by hand.

> Tip: `@EnableMethodSecurity`, `@EnableCaching`, `@Cacheable` and `@CacheEvict`,
> `ApplicationEventPublisher`, `@TransactionalEventListener`, `MeterRegistry` and
> `org.springframework.boot.health.contributor.HealthIndicator`.
