The last pieces a real client needs: headers, tokens, servers that say when to come back,
and callers that change their mind.

- **Headers:** `ClientOptions` gains `headers?: Record<string, string>`, sent on every
  request. They are applied after the client's own (`accept`, `content-type`), so they can
  replace them.
- **Auth:** `ClientOptions` gains `auth?: () => string | Promise<string>`. It is called before
  **every attempt** (a token may expire between retries), and its token goes out as
  `authorization: Bearer <token>`, after `headers`. When it throws (or rejects), nothing is
  sent and the result is `{ kind: "auth"; message: string }`, a new member of `HttpError`
  that is not retried.
- **Retry-After:** when a retryable `status` answer carries a `retry-after` header made of
  digits only, the client waits that many **seconds** before the next attempt instead of the
  backoff delay (`0` means retry at once). Any other value of the header is ignored.
- **Cancelling:** `RequestOptions` and the methods of `createApiClient` take
  `signal?: AbortSignal`. When it aborts, before or during an attempt or while waiting for a
  retry, the request stops at once (no waiting out the backoff) with
  `{ kind: "aborted" }`, also new in `HttpError` and never retried. A signal that is already
  aborted sends nothing. A `timeout` stays a `timeout` when a signal is given but not aborted.
