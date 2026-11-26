# Checkpoint 3: Webhook Inbox

A payment provider calls your service with webhooks: a JSON body and a signature header. The
inbox checks the signature, validates the event, drops duplicates and calls the handlers
registered for its type. Three files are yours to finish.

## vendor/signature.d.ts

`vendor/signature.js` is a plain JavaScript module from the provider, with no types. **Do not
change it.** Its declaration file was written in a hurry with `any` everywhere: describe what
`sign(secret, body)` and `verify(secret, body, signature)` really take and return (read the
`.js` file).

## events.ts

`WebhookEventSchema` is a zod **discriminated union on `type`**. Every event has an `id`
matching `/^evt_[A-Za-z0-9]+$/` and a `createdAt` that is an ISO date-time string, turned into
a `Date` by the schema.

| `type` | `data` |
| --- | --- |
| `payment.succeeded` | `paymentId`, `amountCents` (a positive integer), `currency` (three upper-case letters) |
| `payment.refunded` | `paymentId`, `refundedCents` (a positive integer), an optional `reason` string |
| `customer.created` | `customerId` (a non-empty string), `email` (an email address) |

`PaymentIdSchema` is a string matching `/^pay_[A-Za-z0-9]+$/`, **branded** `"PaymentId"`. The
types below the schema (`WebhookEvent`, `EventType`, `EventOf<T>`) are derived from it and need
no change once the schema is right.

`parseEvent(input: unknown)` returns `{ ok: true, event }` or `{ ok: false, issues }`, with one
`"path: message"` string per issue, the path joined with dots (`"data.currency: ..."`) and
`"(root)"` for an empty path.

## inbox.ts

`new WebhookInbox(secret)`:

- **`on(type, handler)`** registers a handler for one event type, typed so that the handler
  receives the event of that type, and returns a function that removes it.
- **`receive(body, signature)`** returns a `Receipt`, checking in this order:
  1. the signature, with `verify`, **before** reading the body: `rejected` / `"signature"`;
  2. the JSON: a body that does not parse is `rejected` / `"json"` (never an exception);
  3. the schema: `rejected` / `"schema"` with the issues;
  4. duplicates: an event id that was already handled, **or is being handled right now**,
     is a `duplicate`, and no handler runs;
  5. the handlers of that type, in registration order, each awaited before the next. The
     receipt is `accepted` with `handled`, the number of handlers that ran.

  When a handler throws (or rejects), the rest do not run and the receipt is `failed` with
  the error, turned into an `Error` when it is not one. A failed event is **not** marked as
  handled: providers send it again, and the retry must be processed.

`rejected` receipts for the signature and the JSON have `issues: []`.

Import from local files with their extension (`./events.ts`, `./vendor/signature.js`) and mark
type-only imports with `import type`.

Run the tests with `npm test` after `npm install`. There are 22 tests: 1 in
`tests/visible.test.ts` and 21 hidden ones, 6 of them type-level.
