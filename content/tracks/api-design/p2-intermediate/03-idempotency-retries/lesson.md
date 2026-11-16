# Idempotency Keys and Safe Retries

Networks fail in the worst possible place: **after** the server did the work and **before** the
client heard about it. The client sees a timeout and cannot know whether the payment went
through. If it retries a plain `POST`, the customer may be charged twice; if it does not, the
order may never be paid. Idempotent methods (`PUT`, `DELETE`) can simply be retried; for
`POST`, the API must provide a way to make retries safe.

## Retries are inevitable

Retries happen in HTTP libraries, service meshes, load balancers, SDKs, message consumers and
in users pressing the button twice. Design the API on the assumption that **every request may
arrive more than once**.

A good client retry policy:

- retries only what is safe: idempotent methods, or `POST` with an idempotency key;
- retries on network errors, `408`, `429`, `502`, `503`, `504`; never on other `4xx`;
- waits with **exponential backoff and jitter** (for example 0.5 s, 1 s, 2 s, ± random),
  honours `Retry-After`, and gives up after a few attempts or a deadline;
- uses the **same key** for every retry of the same operation.

> The Backend track's "Resilient Client" kata (`backend.p3.resilient-client`) builds such a
> client. This lesson is about what the API must offer so those retries are harmless.

## The Idempotency-Key header

Popularised by Stripe and being standardised by the IETF HTTPAPI working group
(`draft-ietf-httpapi-idempotency-key-header`):

```http
POST /payments HTTP/1.1
Idempotency-Key: 8e03978e-40d5-43e8-bc93-6894a57f9324
Content-Type: application/json

{"account": "acct-1", "amountCents": 2500}
```

The client generates a unique key (a UUID v4 or v7) **per operation**, not per attempt. The
server:

1. Looks up the key **scoped to the client** (two customers may pick the same UUID by
   accident, or on purpose).
2. **New key**: records it as *in progress* together with a **fingerprint** of the request
   (method, path, a hash of the canonical body), runs the operation, stores the response.
3. **Known key, same fingerprint, finished**: returns the **stored response** without running
   the operation again. Many APIs mark it (`Idempotent-Replayed: true`).
4. **Known key, different fingerprint**: `422` (the key was reused for another request).
5. **Known key, still in progress**: `409` with `Retry-After`: a concurrent duplicate.
6. **Missing key** on an endpoint that requires one: `400`.

### What to store and for how long

- Store the outcome of anything the client should not repeat: success **and** business
  failures such as a declined card (a retry must not charge again).
- Do **not** store outcomes that mean "nothing happened": a `503` because the processor was
  unreachable before charging, a crash before the transaction committed. Release the key so the
  client can retry with it.
- Keep keys long enough to cover realistic retries (Stripe: 24 hours) and document it.

### Making it correct under concurrency

Two requests with the same key can arrive at the same moment on two servers. The "is this key
new?" decision must be **atomic**: a unique constraint on `(client, key)` in the database, an
atomic `SET NX` in Redis, `putIfAbsent` in memory. The side effect itself should be committed
together with the stored response (same transaction, or an outbox), or the server can crash
between the two.

## Other ways to get idempotency

| Technique | Example |
| --- | --- |
| Client-chosen ids with `PUT` | `PUT /payments/{clientGeneratedId}` creates or returns the same payment |
| Natural keys and unique constraints | one invoice per `(customerId, period)` |
| Conditional requests | `If-Match` makes a lost update impossible (next lesson) |
| Idempotent consumers | messages carry ids; consumers record processed ids (phase 3, webhooks) |

## Further reading

- IETF HTTPAPI draft, "The Idempotency-Key HTTP Header Field": <https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/>
- Stripe API reference, "Idempotent requests": <https://docs.stripe.com/api/idempotent_requests>
- Brandur Leach, "Implementing Stripe-like Idempotency Keys in Postgres": <https://brandur.org/idempotency-keys>
- AWS Builders' Library, "Making retries safe with idempotent APIs": <https://aws.amazon.com/builders-library/making-retries-safe-with-idempotent-APIs/>
- AWS Builders' Library, "Timeouts, retries, and backoff with jitter": <https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/>
- RFC 9110, section 9.2.2, "Idempotent Methods": <https://www.rfc-editor.org/rfc/rfc9110#section-9.2.2>
