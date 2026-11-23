A phone sends a sign-up, the answer is lost on a train, and the app sends it again. Today
the retry answers `409 already signed up`, or worse, takes a seat someone else freed in
between. Clients now send an `Idempotency-Key` header with `POST /events/{id}/signups`; a
retry carries the same key.

Rules for a sign-up **with** a key (without one, nothing changes):

| Case | Answer |
| --- | --- |
| first time this key is seen | sign up as before; when it succeeds, store the answer under the key **in the same transaction** (a third operation: a `Put` of `IDEMPOTENCY#<key>` / `KEY`, on condition that it does not exist, or that it expired) |
| the same key again, for the same event and email | the stored answer (`201` and the same body), with the header `idempotent-replayed: true`, and **no write**: even if the event filled up, or the person cancelled, in between |
| the same key for another event or email | `422` `idempotency key reused` |
| a key that is not 1 to 64 characters of `A-Z`, `a-z`, `0-9`, `_`, `-` | `400` |
| the sign-up fails (`404`, `409`, `400`) | nothing is stored: a retry with the same key tries again |

Stored answers last **24 hours**. Keep the expiry in an attribute `expires_at`, a number of
**seconds since the epoch** (`int(now()) + 86400`), which is what DynamoDB **TTL** expects
(stage 4 turns TTL on for it). TTL deletes expired items *eventually*, often hours later, so
an item whose `expires_at` has passed must be treated as absent by your code, and may be
overwritten by a new request with the same key.

Read the time through a module-level function `now()` that returns `time.time()`: the tests
replace it to move the clock.

> [!TIP]
> Two retries can race: both find no stored answer and both try the transaction. The
> condition on the idempotency item lets only one win; the loser's `CancellationReasons`
> say the third operation failed, and it can answer with what the winner stored.
