# Quote Pipeline

`QuoteService.quote` takes seven positional parameters (two booleans that are easy to swap),
applies five pricing rules in one method in a fixed order, then calls the audit log and the
sales mail directly. A new rule, a new order of rules, or a new party interested in quotes
means editing this one method.

Use this phase's patterns together:

- a **Builder** for the request, so every value is named and validated in one place;
- a **pipeline of steps** (a chain of responsibility where every link runs), each rule a
  `PricingStep` lambda;
- the **Observer** pattern for whoever wants to hear about quotes, with subscriptions that
  can be closed and listeners that cannot break each other.

`QuoteService` keeps its old method for its callers and becomes a thin layer over the new
engine. The visible tests pin today's prices and pass before you start.

## The target design

| Type | What it does |
| --- | --- |
| `record QuoteRequest(customer, product, quantity, unitCents, vip, coupon, express)` | validates in its compact constructor (messages as today: `"customer is required"`, `"product is required"`, `"quantity must be at least 1"`, `"unit price cannot be negative"`, all `IllegalArgumentException`); `static Builder builder()` with one method per component and `build()`. Defaults: quantity 1, unit price 0, no VIP, no coupon (`null`), no express. |
| `record Quote(customer, cents, adjustments)` | as today, plus `Quote adjusted(long newCents, String adjustment)`: a copy with a new amount and one more adjustment |
| `@FunctionalInterface interface PricingStep` | `Quote apply(QuoteRequest request, Quote quote)`: returns the quote unchanged when the rule does not apply |
| `final class PricingSteps` | `bulkDiscount()`, `vipDiscount()`, `coupon()`, `expressShipping()`, `tax()` and `List<PricingStep> standard()` with the five in today's order |
| `@FunctionalInterface interface QuoteListener` | `void quoted(QuoteRequest request, Quote quote)` |
| `interface Subscription extends AutoCloseable` | `void close()` (no checked exception): stops that listener's notifications |
| `final class QuoteEngine` | `QuoteEngine(List<PricingStep> steps)`; `Quote quote(QuoteRequest)` starts from `quantity * unitCents` with no adjustments, applies the steps in order, then tells each listener; `Subscription subscribe(QuoteListener)`; `int failedNotifications()` |

The rules, as today: bulk (quantity ≥ 10) takes 5 % off, VIP 10 % off (each `cents * p / 100`
rounded down), coupon `SAVE5` takes 500 cents off but never below 0, express adds 1500, and
tax adds `(cents * 23 + 50) / 100`. A listener that throws is counted in
`failedNotifications()` and skipped: the quote and the other listeners carry on.

## What the grading tests check

- Builder defaults and validation; the standard steps price exactly like the old service.
- Each step alone; a step the grader writes can be inserted anywhere in the chain.
- Listeners hear every quote until their subscription is closed; a failing listener breaks
  nothing.
- The old `QuoteService` still writes the same audit entries and mails.
- `QuoteEngine` mentions no rule (`SAVE5`, `vip`, `23` ...) and no listener class.
