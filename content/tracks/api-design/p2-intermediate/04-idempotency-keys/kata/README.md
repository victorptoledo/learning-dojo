# Idempotency Keys

`POST /payments` and `POST /refunds` move money, and clients retry: after a timeout, after a
`503`, after a crash, or because a user pressed the button twice. Without protection every
retry charges again. Write the idempotency layer that both endpoints already call, following
the `Idempotency-Key` header of the IETF HTTPAPI draft.

`Gateway` (counts every charge, declines amounts ending in 13, can be told to fail once),
`Ticker` (a clock the tests move), `Requests`, `IdempotencyExceptions`, `ApiProblems` and
`PaymentController` are written. Only `Idempotency.execute` is yours.

```text
POST /payments
X-Api-Key: shop-1
Idempotency-Key: 8e03978e-40d5-43e8-bc93-6894a57f9324
{"account": "acct-1", "amountCents": 2500}
→ 201 Created, Location: /payments/pay_1, {"id": "pay_1", ..., "chargeId": "ch_7"}

(the same request again)
→ 201 Created, Location: /payments/pay_1, Idempotent-Replayed: true, the same body; no charge
```

## Rules

1. **No key** (missing or blank): `KeyMissingException` (400), and the action does not run.
2. A key belongs to one **client** (`X-Api-Key`): the same key from two clients is two keys.
3. The **fingerprint** of a request is its operation (`"POST /payments"`) plus its body. The
   body arrives as a record, so the same JSON written in another order or spacing is the same
   request.
4. **First use** of a key: remember it as *in progress*, run the action, and store its answer
   (status, headers, body). Return that answer as is.
5. **Same key, same fingerprint, finished**: replay the stored answer with the header
   `Idempotent-Replayed: true`, without running the action.
6. **Same key, different fingerprint**: `KeyReusedException` (422).
7. **Same key, first request still running**: `RequestInProgressException` (409; `ApiProblems`
   adds `Retry-After`).
8. **Failures that did not happen**: if the action throws, or answers a `5xx`, forget the key
   so the client can retry with it. Any other answer, a `402` decline included, is stored and
   replayed like a success.
9. Keys are remembered for **24 hours** after their first use (by `Ticker`): after that the
   same key is a first use again.
10. Ten concurrent requests with one key must run the action **once**; the others get `409`
    (or the replay, if they arrive after it finished).

> Tip: `ConcurrentHashMap.putIfAbsent` decides the race in one step: only the request that
> puts the in-progress entry runs the action. `remove(key, value)` and `replace(key, old, new)`
> only act on the entry you put. Copy the answer's headers with `new HttpHeaders()` and
> `addAll(...)`.
