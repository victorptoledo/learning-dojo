First the organizer puts an event on sale. An event has rows of seats: row `A` is the first,
seats are numbered from 1, so a seat is called `A1`, `A2`, ..., `B1`, ...

| Request | Who | Answer |
| --- | --- | --- |
| `POST /events` `{"name", "startsAt", "rows", "seatsPerRow", "priceCents"}` | `ORGANIZER` | `201`, `Location: .../events/{id}`, the event |
| `GET /events/{id}` | anyone logged in | `200`, the event |
| `GET /events/{id}/seats` | anyone logged in | `200`, every seat `{"seat": "A1", "status": "AVAILABLE"}`, row by row, by number |

```json
{"id": 1, "name": "Opening night", "startsAt": "2099-06-01T20:00:00Z", "priceCents": 4500,
 "seats": 120, "available": 120}
```

Rules:

- `name` is not blank (at most 80 characters), `rows` from 1 to 26, `seatsPerRow` from 1 to
  50, `priceCents` from 0 to 10000000 and `startsAt` is required: `400` otherwise.
- `startsAt` is in the future, by the application `Clock` (`422`, `an event must start in
  the future`). Define a `java.time.Clock` bean and inject it; the tests replace it.
- Every seat is a row in the database (you will lock them later), created with the event.
- `available` counts the seats nobody holds or bought; for now, all of them.
- An unknown event is `404`, `event <id> not found`.
- Security: a stateless API (no session, CSRF off) with HTTP Basic. `POST /events` needs role
  `ORGANIZER` (`403` for buyers); everything else needs a login (`401` without one). Use the
  users of `UsersConfig`.

> Tip: `@Entity` `Event` and `Seat` (a `@ManyToOne(fetch = LAZY)` to its event, a unique
> constraint on event and label, and a `status`); `saveAll` writes the seats; a derived
> `countByEventIdAndStatus` gives `available`.
