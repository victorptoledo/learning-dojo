# Idempotent Payments

`FakeGateway` is the card processor: every `charge` call moves money (it counts them), it is slow
(50 ms), and it declines amounts ending in 13. The entities `Payment` and `IdempotencyRecord` (its
primary key **is** the idempotency key), the records and the error handling are written.

## POST /payments

```text
POST /payments
Idempotency-Key: 6f1c...                    required; missing → 400
{"account": "acct-1", "amountCents": 2500}  validated

→ 201 Created, Location: .../payments/{id}, Idempotent-Replayed: false|true, the payment
```

## Rules (`PaymentService.pay`)

1. **New key**: insert an `IdempotencyRecord(key, request.fingerprint())` **in its own short
   transaction**, then call the gateway (outside any transaction: it is slow and remote), then in
   a second transaction save the `Payment` and `finish(201, paymentId, null)` the record.
2. **Known key, same fingerprint, finished**: **replay**: return the same payment (same id, same
   charge id) with `Idempotent-Replayed: true`, and **do not charge**.
3. **Known key, different fingerprint**: `PaymentException.keyReused()` (**422**).
4. **Known key, not finished yet** (the first request is still running): `inProgress()` (**409**).
5. **Two requests insert the same key at once**: the primary key lets only one insert win; the
   loser catches `DataIntegrityViolationException` and is treated as rule 2, 3 or 4.
6. **Declined**: record `finish(402, null, message)` and throw `declined(message)` (**402**); a
   retry with the same key replays the 402 **without charging again**.

Ten concurrent requests with one key must charge **exactly once**, and each gets `201` or `409`.

> Why `IdempotencyRecord` implements `Persistable`: its id is the key you assign, not a generated
> one, and Spring Data's `save` treats an entity with an id as existing and **merges** it (select,
> then update or insert). Two concurrent "inserts" would then never collide. `isNew()` tells
> Spring Data it is new, so `save` is a real `insert` and the primary key decides the race.

> Tip: `TransactionTemplate` (inject it; Spring Boot provides one) runs a lambda in a transaction:
> `transaction.executeWithoutResult(status -> keys.saveAndFlush(record))`. A `@Transactional`
> method called from the same class would skip the proxy and give you no transaction at all.
