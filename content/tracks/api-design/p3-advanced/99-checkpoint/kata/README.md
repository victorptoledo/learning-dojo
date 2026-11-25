# Checkpoint 3: Partner Exports

Partners download their data as files: orders, customers or invoices, as CSV or NDJSON. The
first version was written in a hurry. The API key travels in the query string, any partner can
read any export, nothing stops a partner from flooding the API, and the export runs **inside**
the request (minutes, in production). Rebuild it so that partners can trust it and it can
trust them.

Written for you:

- `Partners`: registered partners, their scopes and webhook secrets, with keys stored only as
  SHA-256 hashes. `authenticate(key)` finds the partner of a key.
- `Exporter`: builds the file and returns its URL. It is slow, and invoices always fail.
- `BackgroundJobs`: `submit(task)` and `schedule(delay, task)`. The tests call `runDue()` to
  let background work run, and move `Ticker` forward to make delayed jobs due.
- `WebhookSender`: delivers a webhook and returns the status the partner's endpoint answered
  (a fake that records each delivery; the tests can make it answer `503`).
- `Ticker` (the clock: use it for every time you need), `ApiProblems` (with a `problem(...)`
  helper) and the old `ExportController`.

Rework `ExportController`, add what you need, and finish `src/main/resources/openapi.yaml`.
Problem types are `https://api.dojo.example/problems/<type>`, and every problem is
`application/problem+json`.

## Who is calling

Each request to `/exports` goes through these checks, in this order:

1. **The key is never in the URL.** A request with an `apiKey` query parameter is a `400`
   problem `key-in-query`, even when the key is valid.
2. **Authentication.** The key arrives as `Authorization: Bearer <key>`. No header, another
   scheme or an unknown key is a `401` problem `unauthorized` with
   `WWW-Authenticate: Bearer error="invalid_token"`.
3. **Rate limit** (below). Requests that failed 1 or 2 do not count.
4. **Scope.** `POST /exports` needs `exports:write` and `GET /exports/{id}` needs
   `exports:read`. Without it: a `403` problem `insufficient-scope` with
   `WWW-Authenticate: Bearer error="insufficient_scope", scope="exports:write"` (the scope
   that was needed).

## Rate limit: a token bucket per partner

Each partner has a bucket of **10 tokens**, refilled continuously at **one token per
second** (by `Ticker`, to the millisecond) and never above 10. Every request that passes
authentication takes one token. Every answer to such a request (`2xx`, `4xx` and `429` alike)
carries:

```text
RateLimit-Policy: "default";q=10;w=10
RateLimit: "default";r=<whole tokens left>;t=<seconds until the bucket is full, rounded up>
```

With less than one token, the answer is a `429` problem `rate-limited` with `Retry-After:` the
seconds until the next whole token (rounded up, at least 1). A refused request takes nothing.

Ten requests in a row from a fresh partner answer `r=9;t=1`, then `r=8;t=2` ... `r=0;t=10`;
the eleventh is a `429` with `Retry-After: 1` and `r=0;t=10`.

## Exports are long-running operations

```text
POST /exports   {"dataset": "orders", "format": "csv"}
→ 202 Accepted
  Location: /exports/exp_31
  Retry-After: 5
  {"id": "exp_31", "dataset": "orders", "format": "csv", "status": "RUNNING"}
```

- `dataset` is `orders`, `customers` or `invoices` and `format` is `csv` or `ndjson`;
  anything else is a `400` problem, and nothing starts.
- `POST` does **not** call `Exporter`: it stores the export as `RUNNING` and submits a
  background job that runs it.
- When the job runs, the export becomes `SUCCEEDED` with the `resultUrl` from `Exporter`, or
  `FAILED` with an `error` that is a problem: `{"type": ".../problems/export-failed", "title":
  "Export failed", "detail": "<the exception's message>"}`.
- `GET /exports/{id}` answers `200` with the export. While it is `RUNNING` it adds
  `Retry-After: 5`. Fields without a value (`resultUrl`, `error`) are left out.
- **Object-level authorisation:** a partner sees only its own exports. Another partner's
  export is a `404` problem `export-not-found` (title `Export not found`), exactly like an id
  that does not exist.

## Webhooks: the end of every export is announced

When an export finishes (either way), `POST` an event to the partner with
`WebhookSender.deliver(partnerId, headers, body)`, following Standard Webhooks:

```text
content-type: application/json
webhook-id: evt_12
webhook-timestamp: 1780304400
webhook-signature: v1,K5oZfzN95Z9UVu1EsfQmfVNQhnkZ2pj9o9NDN/H/pI4=

{"id": "evt_12", "type": "export.succeeded", "createdAt": "2026-06-01T09:00:00Z",
 "data": {"exportId": "exp_31", "status": "SUCCEEDED", "resultUrl": "https://files..."}}
```

- `type` is `export.succeeded` or `export.failed`; `data` has `exportId` and `status`, plus
  `resultUrl` when it succeeded. `createdAt` is when the export finished (`Instant.toString`).
- `webhook-id` is the event's `id`, unique per event and **the same on every retry**.
- `webhook-timestamp` is the time of **this attempt**, in seconds since the epoch.
- `webhook-signature` is `v1,` followed by the base64 HMAC-SHA256 of
  `<webhook-id>.<webhook-timestamp>.<body>`, over the exact body you send. The key is the
  partner's webhook secret without its `whsec_` prefix, base64-decoded.
- **At-least-once delivery:** when the partner's endpoint does not answer `2xx`, retry after
  10 seconds, then 20, 40 and 80 more: five attempts at most, each with its own timestamp and
  signature.

## The contract

`openapi.yaml` was reviewed before the code, but the review left notes. Keep what is there,
and add:

1. A `security` requirement on **each operation**: `partnerKey` with the scope it needs
   (`exports:write` or `exports:read`).
2. The `401`, `403` and `429` responses on each operation, as `application/problem+json`. The
   `429` declares the headers `Retry-After`, `RateLimit` and `RateLimit-Policy`. Reusable
   responses under `components/responses` with `$ref` are welcome.
3. The `Retry-After` header on the `202`.
4. A `webhooks` section with `exportFinished`: a `post` whose parameters are the three
   required headers `webhook-id`, `webhook-timestamp` and `webhook-signature` (`in: header`,
   `required: true`), and whose `requestBody` is `application/json`.

## What the grading tests check

- Each check of "Who is calling", in order, with its status, problem type and challenge.
- Bucket arithmetic: `r` and `t` after each request, `429` and `Retry-After`, refills, one
  bucket per partner, and the limit applied before the scope.
- `202`, `Location` and `Retry-After`; `Exporter` not called during `POST`; `RUNNING`,
  `SUCCEEDED` and `FAILED` exports; `404` for someone else's export.
- Each webhook: its headers, its signature (recomputed by the tests), its body, the retry
  schedule, and that it stops after five attempts or one success.
- `openapi.yaml`, read with SnakeYAML (local `$ref`s are followed).

> Tip: one `HandlerInterceptor` (added in a `WebMvcConfigurer`) can do the four checks before
> any handler runs and hand the partner to the controller in a request attribute. An exception
> thrown from `preHandle` still reaches your `@ExceptionHandler`s, and headers you set on the
> response before throwing are kept.
