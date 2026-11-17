A buyer first **holds** seats: they are kept for a few minutes while the buyer pays (the
next stages make holds expire and turn them into orders).

| Request | Who | Answer |
| --- | --- | --- |
| `POST /events/{id}/holds` `{"seats": ["A1", "A2"]}` | `USER` | `201`, `Location: .../holds/{id}`, the hold |
| `GET /holds/{id}` | its buyer | `200`, the hold |
| `DELETE /holds/{id}` | its buyer | `204`; its seats are available again and the hold is `RELEASED` |

```json
{"id": 7, "eventId": 1, "seats": ["A1", "A2"], "totalCents": 9000,
 "expiresAt": "2099-01-01T10:10:00Z", "status": "HELD"}
```

The settings are already in `application.properties`; read them into a
`@ConfigurationProperties("tickets")` record (the tests use other values):

```properties
tickets.hold-ttl=PT10M
tickets.max-seats-per-hold=6
```

Rules, checked in this order:

1. The body has from 1 to `max-seats-per-hold` seats and no seat twice: `400`, with the
   error on `seats`.
2. The event exists (`404`) and every seat exists in it: `422`, `unknown seat Z9`.
3. **All or nothing:** if any of the seats is not `AVAILABLE`, nothing is held and the answer
   is `409` with an `unavailable` property listing those seats, in seat order.
4. The hold gets `seats` in seat order (row, then number), `totalCents` = price × seats,
   `expiresAt` = now (by the `Clock`) + `hold-ttl`, and `status` `HELD`. Its seats become `HELD`,
   and the event `available` count goes down.

- Only buyers hold seats (`403` for the organizer).
- Another buyer's hold, or one that does not exist, is `404` (`hold <id> not found`).
- Releasing a hold that is not `HELD` any more is `409`.

**Under load.** The tests send two dozen hold requests at the same moment, over real
connections, for the same seat or for overlapping pairs of seats. Exactly one request may win
each seat, the others get `409`, never `500`, and no seat ends up in two holds.

Checking "is it available?" and then writing "held" is two steps, and another request fits
between them. Lock the seat rows first (`PESSIMISTIC_WRITE`, `select ... for update`), in one
fixed order so two holds that share seats cannot wait for each other forever, and decide
after the lock.

> Tip: a repository method with `@Lock(LockModeType.PESSIMISTIC_WRITE)` and `@Query("select s from
> Seat s where s.event.id = :event and s.label in :labels order by s.id")`; `@ConfigurationPropertiesScan`
> on the application class; `ProblemDetail.setProperty("unavailable", seats)`.
