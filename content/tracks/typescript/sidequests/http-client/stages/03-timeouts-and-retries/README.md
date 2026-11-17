Networks fail. Make the client wait less for a dead server and try again when that is safe.

- `ClientOptions` gains `timeoutMs?: number`, `retries?: number` (default `0`) and
  `retryDelayMs?: number` (default `100`). `HttpError` gains `{ kind: "timeout"; ms: number }`.
- **Timeout:** with `timeoutMs`, an attempt that has not finished after that many
  milliseconds (the body included) is abandoned and fails with `{ kind: "timeout", ms }`.
  Without it, an attempt waits as long as it takes.
- **What is worth retrying:** a `network` error, a `timeout`, or a `status` error with status
  `429`, `502`, `503` or `504`. Nothing else: not a `decode` error, not a `500`, not a `4xx`.
- **What is safe to retry:** `GET`, `PUT` and `DELETE` are idempotent; `POST` and `PATCH` are
  not. `RequestOptions` and `Endpoint` gain `idempotent?: boolean`, which overrides that in
  both directions (a `POST` with an idempotency key can say `true`, a `GET` that sends an
  e-mail can say `false`).
- **How:** a retryable failure of a safe request is tried again, up to `retries` more times.
  Before retry number `n` (1, 2, 3...) the client waits `retryDelayMs * 2 ** (n - 1)`
  milliseconds: 100, 200, 400 with the defaults. When the attempts run out, the result is the
  last attempt's error.
- `createApiClient` passes `idempotent` from the endpoint and uses the same options.
