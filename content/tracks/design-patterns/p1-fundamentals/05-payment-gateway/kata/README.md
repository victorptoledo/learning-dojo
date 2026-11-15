# Payment Gateway

`CheckoutService.pay` takes payments through `LegacyPayClient`, the provider's 2011 SDK. It
converts cents to the SDK's `"12.50"` strings, reads its status codes, retries when the
provider is busy and counts successes and failures, all in one method. You cannot reuse the
retry for another provider, meter without retrying, or test checkout without the SDK.

Refactor it with two structural patterns:

- an **Adapter** that makes the SDK look like an interface in our own terms, and
- **Decorators** that add retrying and metering by wrapping any gateway behind the same
  interface, so they can be stacked in any order.

`LegacyPayClient` is a library: do not change it.

## The target design

| Type | What it does |
| --- | --- |
| `interface PaymentGateway` | `Receipt charge(Payment payment)`; throws `PaymentDeclinedException` or `GatewayUnavailableException` |
| `LegacyPayAdapter implements PaymentGateway` | `LegacyPayAdapter(LegacyPayClient client)`. Sends the amount as `"%d.%02d"` (2599 cents → `"25.99"`) with the account and currency; `OK` gives `new Receipt(client.lastReference(), cents)`, `DECLINED` throws `PaymentDeclinedException`, `BUSY` (or any other code) throws `GatewayUnavailableException`. |
| `RetryingGateway implements PaymentGateway` | `RetryingGateway(PaymentGateway inner, int maxAttempts)`; tries up to `maxAttempts` times while `inner` throws `GatewayUnavailableException`, then rethrows the last one. Never retries a decline. `maxAttempts < 1` throws `IllegalArgumentException`. |
| `MeteredGateway implements PaymentGateway` | `MeteredGateway(PaymentGateway inner)`; `int successes()` and `int failures()` count calls that returned or threw (any `RuntimeException`, which it rethrows unchanged) |
| `CheckoutService` | `CheckoutService(PaymentGateway gateway)`; `pay(payment)` refuses amounts `<= 0` with `IllegalArgumentException`, otherwise delegates |

The counters move from `CheckoutService` to `MeteredGateway`. Where you put the meter now
matters, and that is the point: outside the retries it counts payments, inside it counts
attempts.

```java
var gateway = new MeteredGateway(new RetryingGateway(new LegacyPayAdapter(new LegacyPayClient()), 3));
var checkout = new CheckoutService(gateway);
```

## What the grading tests check

- The adapter's translation of amounts and status codes, with the grader's own fake SDK.
- Retries stop at the first success or the last attempt, and a decline is tried once.
- Decorators wrap lambdas and each other; both orders of meter and retry give the counts above.
- `CheckoutService` holds only a `PaymentGateway`, and only `LegacyPayAdapter` mentions
  `LegacyPayClient`.

The visible tests in `src/test/java` describe the target design, so they do not compile until
the new types exist.
