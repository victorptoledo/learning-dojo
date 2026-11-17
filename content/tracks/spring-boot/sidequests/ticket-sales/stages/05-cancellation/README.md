The show is off. The organizer cancels the event, and every buyer gets their money back,
including the ones whose purchase was in flight at that very moment.

| Request | Who | Answer |
| --- | --- | --- |
| `DELETE /events/{id}` | `ORGANIZER` | `200`, `{"refundedOrders": 3, "refundedCents": 13500, "releasedHolds": 2}` |
| `GET /events/{id}/report` | `ORGANIZER` | `200`, the numbers of the event |

Every event now has a `status`: `ON_SALE`, or `CANCELLED` after the cancellation.

**Cancelling**, all in one transaction:

- every `PAID` order of the event becomes `REFUNDED` (`refundedOrders`, `refundedCents`);
- every hold that is still active becomes `RELEASED` (`releasedHolds` counts them);
- every seat is `AVAILABLE` in the seat list, but nothing can be held any more: `available`
  is 0, a new hold is `409` (`event <id> is cancelled`), and so is a purchase;
- cancelling twice is `409` (`event <id> is already cancelled`); an unknown event is `404`;
  buyers get `403`.

A retry of a purchase that was made **before** the cancellation, with the same idempotency
key, still answers `200` with that order, now `REFUNDED`.

**The report:**

```json
{"status": "ON_SALE", "seats": 10, "available": 5, "held": 3, "sold": 2,
 "paidOrders": 1, "revenueCents": 1000, "refundedOrders": 0, "refundedCents": 0}
```

`held` counts the seats of holds that are still active; `sold` the seats of paid orders;
`revenueCents` adds up the `PAID` orders and `refundedCents` the `REFUNDED` ones.

**Under load.** The tests start a dozen purchases and the cancellation at the same moment.
Whatever the order they run in, at the end there is **no paid order** for the event: every
purchase that answered `201` was refunded by the cancellation, and every one that came after
it was refused. `refundedOrders` is exactly the number of `201` answers.

The danger is a purchase that checked "is the event on sale?" just before the cancellation
committed, and then wrote its order just after. Make the cancellation and every change to
the event's seats take the **same lock first**, the event row, so they happen one after the
other, and always in the same order (event, then hold, then seats), so they never wait for
each other in a circle.

One trap: to know which event to lock, you read the hold first. If that read loads the
`Hold` entity, the later `select ... for update` of the same hold hands you back **the same
object, with the state it had before you waited**, and two purchases both see `HELD`. Read
only the event id (a scalar query), or refresh the entity after locking it.

> Tip: a bulk JPQL `update ... set o.status = REFUNDED where o.eventId = :id and o.status = PAID`
> returns how many rows it changed; read the sums before you change them.
