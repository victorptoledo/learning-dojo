Rooms can now be booked. A booking has an `id`, the `roomId`, a `title`, a `start`, an
`end` and the number of `attendees`.

| Request | Answer |
| --- | --- |
| `POST /rooms/{roomId}/bookings` `{"title", "start", "end", "attendees"}` | `201`, `Location: .../bookings/{id}`, the booking |
| `GET /bookings/{id}` | `200`, the booking |
| `DELETE /bookings/{id}` | `204`: the time is free again |
| `GET /rooms/{roomId}/bookings` | `200`, the bookings of the room, by start |
| `GET /rooms/{roomId}/bookings?date=2099-03-02` | `200`, only the ones that start on that day, by start |

```json
{"title": "Standup", "start": "2099-03-02T09:00", "end": "2099-03-02T09:30", "attendees": 5}
```

Rules, checked in this order:

1. The body is valid: `title` not blank (at most 80 characters), `start` and `end` present,
   `attendees` at least 1, and `end` **after** `start` (otherwise the error is on `end`). A bad
   body is `400` with the `errors` object, as for rooms.
2. The room exists (`404`, `room <id> not found`).
3. The people fit: more attendees than the capacity is `422` with the detail
   `room <name> holds <capacity> people`.
4. No overlap: a booking that shares any minute with another booking **of the same room**
   is `409`, and the Problem Detail has a `conflictsWith` property with the id of that
   booking. Back to back is fine: one may end at 10:00 and the next start at 10:00.

- An unknown booking is `404` with the detail `booking <id> not found`.
- A room that still has bookings cannot be deleted: `409`.
- Two requests for the same free slot at the same moment: only one of them gets it.

> Tip: `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date` reads the query
> parameter; `ProblemDetail.setProperty("conflictsWith", id)` adds the extra field;
> `HttpStatus.UNPROCESSABLE_CONTENT` is 422. Two ranges overlap when each one starts before
> the other ends.
