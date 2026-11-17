A hold becomes an **order** when the buyer pays. Payments go over flaky networks: the app
times out, the buyer taps again, a proxy retries. Each retry must find the payment already
made, not make a second one. The client sends the same **idempotency key** with every retry
of one purchase, and the server answers every retry with the same order.

| Request | Who | Answer |
| --- | --- | --- |
| `POST /holds/{id}/purchase` with header `Idempotency-Key: <key>` | its buyer | `201`, `Location: .../orders/{id}`, the order |
| the same request again, with the same key | its buyer | `200`, the **same** order; nothing else happens |
| `GET /orders/{id}` | its buyer | `200`, the order |

```json
{"id": 3, "holdId": 7, "eventId": 1, "seats": ["A1", "A2"], "totalCents": 9000, "status": "PAID"}
```

Rules:

- The key is required: 8 to 64 characters of letters, digits, `-` and `_` (`400` otherwise).
  Keys belong to a buyer: two buyers may use the same key without meeting.
- The hold must be the buyer's (`404`, `hold <id> not found`), and `HELD`:
  - expired: `410`, `hold <id> has expired`;
  - released: `409`, `hold <id> is not active`;
  - bought already with another key: `409`, `hold <id> is already purchased`.
- A key the buyer already used for **another** hold is `422`, `idempotency key already used
  for another purchase`.
- Paying turns the seats `SOLD` and the hold `PURCHASED`; a sold seat is never free again.
- Another buyer's order, or one that does not exist, is `404` (`order <id> not found`).

**Under load.** The tests fire a dozen purchases of one hold at the same moment:

- all with the **same** key: exactly one `201`, all the others `200`, and every answer has
  the same order id;
- each with a **different** key: exactly one `201` and the others `409`.

Both come down to the same rule: while one purchase of a hold is being decided, the others
for that hold wait, and only then look for an existing order. A unique constraint on the
buyer and the key keeps the database honest if something slips past.

> Tip: lock the hold row first; then look the key up; then check the status. `@RequestHeader("Idempotency-Key")`
> reads the header (a missing one is already a `400` Problem Detail). Name the order table
> something like `ticket_order`, since `order` is an SQL keyword.
