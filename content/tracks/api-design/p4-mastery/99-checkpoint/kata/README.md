# Billing API: Six Incidents (final exam)

Companies (tenants) bill their customers through this API, and a mobile app lets those
customers pay. Last month went badly: six incident reports are on your desk. Fix each one at
its root, so that it cannot come back, and keep the mobile app's contract:
`src/test/java/dojo/MobileAppContractTest.java` fails today and must pass. Do not change it.

Written for you: `Tenants` (keys stored as hashes), `Authentication` (every request needs
`Authorization: Bearer <key>`; the tenant is the request attribute `Authentication.TENANT`),
`Invoice` (the domain record, which was always right), `InvoiceStore` (by id, with
`ofTenantAfter(tenant, afterId, limit)`), `PaymentGateway` (counts charges; the tests can make
the next one fail) and `Ticker`. Rework `InvoiceController`, `PaymentController` and
`ApiProblems`, and add what you need. Problem types are
`https://api.dojo.example/problems/<type>`, as `application/problem+json`.

## INC-1: "The app shows 0.00 and wrong due dates"

A cleanup refactoring "simplified" the representation: `amountCents` became `amount` (a
`double` in euros), the due date became a local date-time and `issuedAt` was written in
Lisbon time without an offset. Every client broke, and some amounts were off by a cent.

- Restore version 1: `{"id", "customer", "amountCents", "currency", "status", "dueDate",
  "issuedAt"}`, where `amountCents` is an integer, `dueDate` a calendar day (`"2026-06-30"`)
  and `issuedAt` an RFC 3339 instant in UTC (`"2026-06-01T09:00:00Z"`).
- `POST /invoices` takes `{"customer", "amountCents", "currency", "dueDate"}` again
  (`customer` not blank, `amountCents` positive, `currency` three capital letters, `dueDate`
  required) and answers `201` with a `Location`. A body without `amountCents` is a `400`
  problem.
- **Root cause:** no `double`, `float`, `Double`, `Float` or `LocalDateTime` anywhere in
  `src/main/java` (comments and strings do not count). Money is a `long` of cents; instants are
  `Instant`.

## INC-2: "The nightly export took the database down"

`GET /invoices` returned every invoice of a tenant (northwind has 5,000).

- Pages of `limit` invoices, **by id, oldest first**: default 50, at most 200. A `limit`
  outside 1 to 200 is a `400` problem `invalid-parameter`.
- The body stays `{"data": [...]}` and gains `"nextCursor"`: an **opaque** cursor (not a
  plain number) when there are more invoices, `null` on the last page. A page with a next
  cursor also sends `Link: <the same URL with cursor=...>; rel="next"`.
- `?cursor=` continues after the cursor's position, so walking the pages sees every invoice
  exactly once, even while new invoices are created in between. A cursor this API did not
  issue is a `400` problem `invalid-cursor`.

## INC-3: "Our database address was on a customer's screenshot"

During an earlier incident someone made every error return `e.toString()` and its stack
trace. Unexpected errors must now be a `500` problem `internal-error` with a generic detail:
no exception names, messages, class or package names, connection strings or traces. Log the
exception, and put an id in `instance` (for example `urn:uuid:...`) that support can find in
the logs. The `401` problem stays as it is.

## INC-4: "A customer saw another company's invoices"

`GET /invoices/{id}` and the payment looked invoices up by id alone. A tenant sees and pays
only its **own** invoices: another tenant's invoice is a `404` problem `invoice-not-found`
(title `Invoice not found`), exactly like an id that does not exist, and is never charged.

## INC-5: "Customers were charged twice"

The mobile app retries `POST /invoices/{id}/payments` (`{"amountCents": ...}`) when the
network is slow, and every retry charged again.

1. A payment needs an `Idempotency-Key` header; missing or blank is a `400` problem
   `idempotency-key-missing`, and nothing is charged.
2. A key belongs to one tenant. The first successful payment with a key is remembered: the
   same key with the same invoice and amount **replays** it (`201`, the same body, and
   `Idempotent-Replayed: true`) without charging.
3. The same key with another invoice or amount is a `422` problem `idempotency-key-reused`.
4. With a new key: a paid invoice is a `409` problem `invoice-already-paid`, and an amount
   that is not the invoice's is a `422` problem `amount-mismatch`. Nothing is charged.
5. A payment that failed (any error) leaves its key free: the client may retry with it.

A successful payment answers `201` with `Location` and `{"id", "invoiceId", "amountCents",
"currency", "chargeId"}`, and the invoice becomes `PAID`.

## INC-6: "The outage that would not end"

When the card processor was down (`GatewayUnavailableException`), the API answered `500` at
once and every app retried at once, for an hour. Answer a `503` problem
`payment-provider-unavailable` with `Retry-After: 30`. Nothing was charged, the invoice stays
`OPEN`, and the same key works when the client retries later.

## What the grading tests check

- `MobileAppContractTest` and the v1 shape, exact cents and UTC instants, and the sources
  (INC-1).
- Default and maximum page sizes, the cursor, the `Link` header and a walk while invoices are
  added (INC-2).
- The body of a `500` (INC-3), `404`s and charges across tenants (INC-4), every rule of INC-5
  with the gateway's charge count, and the `503` and the retry after it (INC-6).

> Tip: write one short post-mortem line per incident before you code (cause, fix, the test
> that proves it). It is the habit the governance lesson asks for, and it keeps the six fixes
> from blurring into one big change.
